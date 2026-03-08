import httpx
import streamlit as st

# ── Page config ────────────────────────────────────────────────────────────────
st.set_page_config(
    page_title="RICS Compliance Assistant",
    page_icon="🏛️",
    layout="wide",
    initial_sidebar_state="expanded",
)

API_BASE = "http://localhost:8000"

# ── Custom CSS ─────────────────────────────────────────────────────────────────
st.markdown("""
<style>
    /* Overall theme */
    .main { background-color: #f8f9fa; }

    /* Header bar */
    .header-bar {
        background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
        padding: 1.5rem 2rem;
        border-radius: 12px;
        margin-bottom: 1.5rem;
        color: white;
    }
    .header-bar h1 { margin: 0; font-size: 1.8rem; font-weight: 700; }
    .header-bar p { margin: 0.3rem 0 0 0; opacity: 0.8; font-size: 0.95rem; }

    /* Chat message styling */
    .source-card {
        background: #ffffff;
        border: 1px solid #e0e0e0;
        border-radius: 8px;
        padding: 0.8rem 1rem;
        margin: 0.4rem 0;
        font-size: 0.85rem;
        border-left: 4px solid #0f3460;
    }
    .source-card .rule-id {
        font-weight: 700;
        color: #0f3460;
        font-size: 0.9rem;
    }
    .source-card .score {
        color: #6c757d;
        font-size: 0.8rem;
        float: right;
    }
    .source-card .section-info {
        color: #495057;
        font-size: 0.8rem;
        margin-top: 0.2rem;
    }

    /* Sidebar styling */
    [data-testid="stSidebar"] {
        background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
    }
    [data-testid="stSidebar"] * { color: #e0e0e0 !important; }
    [data-testid="stSidebar"] .stSelectbox label { color: #a0a0c0 !important; }

    /* Status indicator */
    .status-dot {
        display: inline-block;
        width: 8px; height: 8px;
        border-radius: 50%;
        margin-right: 6px;
    }
    .status-online { background-color: #28a745; }
    .status-offline { background-color: #dc3545; }
</style>
""", unsafe_allow_html=True)

# ── Header ─────────────────────────────────────────────────────────────────────
st.markdown("""
<div class="header-bar">
    <h1>🏛️ RICS Compliance Assistant</h1>
    <p>AI-powered audit assistant grounded on RICS Professional Standards for responsible AI use in surveying</p>
</div>
""", unsafe_allow_html=True)

# ── Sidebar ────────────────────────────────────────────────────────────────────
with st.sidebar:
    st.markdown("### ⚙️ Settings")

    # Health check
    try:
        health = httpx.get(f"{API_BASE}/health", timeout=3).json()
        if health.get("status") == "ok":
            st.markdown('<span class="status-dot status-online"></span> API Online', unsafe_allow_html=True)
        else:
            st.markdown('<span class="status-dot status-offline"></span> API Error', unsafe_allow_html=True)
    except Exception:
        st.markdown('<span class="status-dot status-offline"></span> API Offline', unsafe_allow_html=True)

    st.markdown("---")

    section_filter = st.selectbox(
        "Filter by RICS Section",
        options=[None, "1.2", "2", "3.1", "3.2", "3.3", "4.1", "4.2", "4.3", "4.4", "5"],
        format_func=lambda x: "All Sections" if x is None else f"Section {x}",
    )

    result_count = st.slider("Number of source rules", min_value=1, max_value=10, value=5)

    st.markdown("---")
    st.markdown("### 📋 Quick Questions")
    quick_questions = [
        "What are the data privacy requirements?",
        "What training must staff receive?",
        "What goes in the risk register?",
        "What due diligence is needed for AI procurement?",
        "How should output reliability be assessed?",
        "What must terms of engagement include?",
    ]
    for q in quick_questions:
        if st.button(q, use_container_width=True):
            st.session_state["pending_question"] = q

    st.markdown("---")
    st.markdown(
        "<small>Powered by Qdrant + GPT | RICS Standard (Sept 2025)</small>",
        unsafe_allow_html=True,
    )

# ── Session state ──────────────────────────────────────────────────────────────
if "messages" not in st.session_state:
    st.session_state.messages = []

# ── Chat display ───────────────────────────────────────────────────────────────
for msg in st.session_state.messages:
    with st.chat_message(msg["role"], avatar="🏛️" if msg["role"] == "assistant" else "👤"):
        st.markdown(msg["content"])
        if msg.get("sources"):
            with st.expander(f"📎 {len(msg['sources'])} source rules cited"):
                for s in msg["sources"]:
                    p = s.get("payload", {})
                    st.markdown(
                        f"""<div class="source-card">
                            <span class="rule-id">{s.get('rule_id', '?')}</span>
                            <span class="score">Score: {s.get('score', 0):.4f}</span>
                            <div class="section-info">Section {p.get('section', '?')} — {p.get('section_title', '')}</div>
                            <div style="margin-top:0.4rem; font-size:0.82rem;">{p.get('requirement_text', '')[:200]}{'...' if len(p.get('requirement_text', '')) > 200 else ''}</div>
                        </div>""",
                        unsafe_allow_html=True,
                    )

# ── Handle input ───────────────────────────────────────────────────────────────
pending = st.session_state.pop("pending_question", None)
user_input = st.chat_input("Ask about RICS AI compliance requirements...")

prompt = pending or user_input

if prompt:
    # Display user message
    st.session_state.messages.append({"role": "user", "content": prompt})
    with st.chat_message("user", avatar="👤"):
        st.markdown(prompt)

    # Call API
    with st.chat_message("assistant", avatar="🏛️"):
        with st.spinner("Retrieving RICS rules and generating answer..."):
            try:
                payload = {
                    "message": prompt,
                    "limit": result_count,
                }
                if section_filter:
                    payload["section"] = section_filter

                resp = httpx.post(
                    f"{API_BASE}/chat/",
                    json=payload,
                    timeout=60,
                )
                resp.raise_for_status()
                data = resp.json()

                answer = data.get("answer", "No response received.")
                sources = data.get("sources", [])

                st.markdown(answer)

                if sources:
                    with st.expander(f"📎 {len(sources)} source rules cited"):
                        for s in sources:
                            p = s.get("payload", {})
                            st.markdown(
                                f"""<div class="source-card">
                                    <span class="rule-id">{s.get('rule_id', '?')}</span>
                                    <span class="score">Score: {s.get('score', 0):.4f}</span>
                                    <div class="section-info">Section {p.get('section', '?')} — {p.get('section_title', '')}</div>
                                    <div style="margin-top:0.4rem; font-size:0.82rem;">{p.get('requirement_text', '')[:200]}{'...' if len(p.get('requirement_text', '')) > 200 else ''}</div>
                                </div>""",
                                unsafe_allow_html=True,
                            )

                st.session_state.messages.append({
                    "role": "assistant",
                    "content": answer,
                    "sources": sources,
                })

            except httpx.HTTPStatusError as e:
                st.error(f"API error: {e.response.status_code} — {e.response.text}")
            except httpx.ConnectError:
                st.error("Cannot connect to the API. Make sure the FastAPI server is running on localhost:8000.")
            except Exception as e:
                st.error(f"Unexpected error: {e}")
