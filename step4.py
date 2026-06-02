f = open("src/app/features/chat/chat.component.scss", "w", encoding="utf-8")
f.write("""
$accent: var(--color-accent, rgb(var(--accent-rgb)));

.chat-panel {
  display: flex; flex-direction: column; height: 100%;
  background: rgb(var(--bg-card-rgb));
  border-left: 1px solid rgb(var(--fg-rgb) / .07);
  overflow: hidden;
}

.chat-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 16px; border-bottom: 1px solid rgb(var(--fg-rgb) / .07); flex-shrink: 0;
  &__title { display: flex; align-items: center; gap: 8px; font-size: .85rem; font-weight: 700; color: rgb(var(--fg-strong-rgb)); svg { color: $accent; } }
  &__clear { background: none; border: none; cursor: pointer; color: rgb(var(--fg-rgb) / .35); display: flex; align-items: center; padding: 4px; border-radius: 5px; transition: all .2s;
    &:hover { color: rgb(var(--fg-rgb) / .7); background: rgb(var(--fg-rgb) / .06); } }
}

.chat-messages { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 12px; min-height: 0; }

.chat-empty {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: 24px 16px; gap: 8px;
  &__icon { width: 56px; height: 56px; display: flex; align-items: center; justify-content: center; background: color-mix(in srgb, $accent 8%, transparent); border-radius: 50%; margin-bottom: 8px; svg { color: $accent; opacity: .6; } }
  &__title { font-size: .9rem; font-weight: 600; color: rgb(var(--fg-strong-rgb)); margin: 0; }
  &__sub { font-size: .78rem; color: rgb(var(--fg-rgb) / .45); margin: 0; line-height: 1.5; }
}

.chat-suggestions { display: flex; flex-wrap: wrap; gap: 6px; justify-content: center; margin-top: 12px; }
.suggestion-chip { font-size: .72rem; padding: 5px 10px; border-radius: 100px; background: color-mix(in srgb, $accent 10%, transparent); border: 1px solid color-mix(in srgb, $accent 20%, transparent); color: $accent; cursor: pointer; transition: all .2s;
  &:hover { background: color-mix(in srgb, $accent 18%, transparent); } }

.chat-msg {
  display: flex; flex-direction: column; max-width: 90%;
  &--user { align-self: flex-end; align-items: flex-end;
    .chat-msg__bubble { background: $accent; color: #fff; border-radius: 16px 16px 4px 16px; } }
  &--assistant { align-self: flex-start; align-items: flex-start; max-width: 95%;
    .chat-msg__bubble { background: rgb(var(--fg-rgb) / .05); border: 1px solid rgb(var(--fg-rgb) / .08); border-radius: 4px 16px 16px 16px; color: rgb(var(--fg-strong-rgb)); } }
  &__bubble { padding: 10px 14px; font-size: .83rem; line-height: 1.6; word-break: break-word; }
  &__loading { display: flex; gap: 4px; padding: 12px 16px; background: rgb(var(--fg-rgb) / .05); border-radius: 4px 16px 16px 16px;
    span { width: 6px; height: 6px; border-radius: 50%; background: rgb(var(--fg-rgb) / .3); animation: dot-bounce .9s infinite;
      &:nth-child(2) { animation-delay: .15s; } &:nth-child(3) { animation-delay: .3s; } } }
}

@keyframes dot-bounce {
  0%, 80%, 100% { transform: scale(1); opacity: .5; }
  40% { transform: scale(1.3); opacity: 1; }
}

.chat-sources {
  margin-top: 6px;
  &__toggle { display: flex; align-items: center; gap: 5px; font-size: .72rem; color: $accent; background: none; border: none; cursor: pointer; padding: 2px 0; opacity: .7; transition: opacity .2s; &:hover { opacity: 1; } }
  &__list { margin-top: 8px; display: flex; flex-direction: column; gap: 8px; }
}

.source-item { padding: 8px 10px; background: rgb(var(--fg-rgb) / .04); border: 1px solid rgb(var(--fg-rgb) / .08); border-left: 3px solid $accent; border-radius: 4px; font-size: .72rem;
  .source-id { font-family: monospace; color: $accent; font-weight: 600; margin-right: 8px; }
  .source-section { color: rgb(var(--fg-rgb) / .4); }
  .source-text { margin: 4px 0 0; color: rgb(var(--fg-rgb) / .6); line-height: 1.5; }
}

.chat-input-wrap { display: flex; align-items: flex-end; gap: 8px; padding: 12px 14px; border-top: 1px solid rgb(var(--fg-rgb) / .07); flex-shrink: 0; }
.chat-input { flex: 1; resize: none; border: 1px solid rgb(var(--fg-rgb) / .12); border-radius: 10px; padding: 9px 12px; font-size: .83rem; font-family: inherit; background: rgb(var(--fg-rgb) / .04); color: rgb(var(--fg-strong-rgb)); outline: none; transition: border-color .2s; max-height: 120px; overflow-y: auto;
  &:focus { border-color: color-mix(in srgb, $accent 50%, transparent); } &::placeholder { color: rgb(var(--fg-rgb) / .3); } &:disabled { opacity: .5; } }
.chat-send { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; background: $accent; border: none; border-radius: 9px; cursor: pointer; color: #fff; flex-shrink: 0; transition: all .2s;
  &:hover:not(:disabled) { opacity: .85; } &:disabled { opacity: .35; cursor: not-allowed; } }
""")
f.close()
print("chat.component.scss done")
