f = open("src/app/features/chat/chat.component.html", "w", encoding="utf-8")
f.write("""<div class=\"chat-panel\">
  <div class=\"chat-header\">
    <div class=\"chat-header__title\">
      <svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\" width=\"16\" height=\"16\">
        <path d=\"M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z\"/>
      </svg>
      <span>AI Audit Assistant</span>
    </div>
    <button class=\"chat-header__clear\" (click)=\"chatService.clearMessages()\" title=\"Clear\">
      <svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" width=\"14\" height=\"14\">
        <polyline points=\"3 6 5 6 21 6\"/>
        <path d=\"M19 6l-1 14H6L5 6\"/><path d=\"M9 6V4h6v2\"/>
      </svg>
    </button>
  </div>

  <div class=\"chat-messages\">
    <div class=\"chat-empty\" *ngIf=\"messages.length === 0\">
      <div class=\"chat-empty__icon\">
        <svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.2\" width=\"40\" height=\"40\">
          <path d=\"M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z\"/>
        </svg>
      </div>
      <p class=\"chat-empty__title\">Ask me anything about this audit.</p>
      <p class=\"chat-empty__sub\">I can help analyze data, check compliance rules, identify risks, or suggest decisions.</p>
      <div class=\"chat-suggestions\">
        <button class=\"suggestion-chip\" *ngFor=\"let s of suggestions\" (click)=\"sendMessage(s)\">{{ s }}</button>
      </div>
    </div>

    <ng-container *ngFor=\"let msg of messages; let i = index\">
      <div class=\"chat-msg chat-msg--user\" *ngIf=\"msg.role === &apos;user&apos;\">
        <div class=\"chat-msg__bubble\">{{ msg.content }}</div>
      </div>
      <div class=\"chat-msg chat-msg--assistant\" *ngIf=\"msg.role === &apos;assistant&apos;\">
        <div class=\"chat-msg__loading\" *ngIf=\"msg.loading\"><span></span><span></span><span></span></div>
        <div class=\"chat-msg__bubble\" *ngIf=\"!msg.loading\" [innerHTML]=\"formatAnswer(msg.content)\"></div>
        <div class=\"chat-sources\" *ngIf=\"!msg.loading && msg.sources && msg.sources.length > 0\">
          <button class=\"chat-sources__toggle\" (click)=\"toggleSources(i)\">
            <svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" width=\"12\" height=\"12\">
              <path d=\"M12 2L2 7l10 5 10-5-10-5z\"/><path d=\"M2 17l10 5 10-5\"/><path d=\"M2 12l10 5 10-5\"/>
            </svg>
            {{ showSources[i] ? &apos;Hide&apos; : &apos;Show&apos; }} {{ msg.sources.length }} RICS source(s)
          </button>
          <div class=\"chat-sources__list\" *ngIf=\"showSources[i]\">
            <div class=\"source-item\" *ngFor=\"let s of msg.sources\">
              <span class=\"source-id\">{{ s.rule_id }}</span>
              <span class=\"source-section\">{{ s.payload?.section }}</span>
              <p class=\"source-text\">{{ s.payload?.requirement_text }}</p>
            </div>
          </div>
        </div>
      </div>
    </ng-container>
    <div #messagesEnd></div>
  </div>

  <div class=\"chat-input-wrap\">
    <textarea class=\"chat-input\" [(ngModel)]=\"inputText\" (keydown)=\"onKeydown($event)\"
      placeholder=\"Ask about this audit... (Enter to send)\" rows=\"1\" [disabled]=\"isLoading\">
    </textarea>
    <button class=\"chat-send\" (click)=\"sendMessage()\" [disabled]=\"!inputText.trim() || isLoading\">
      <svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" width=\"16\" height=\"16\">
        <line x1=\"22\" y1=\"2\" x2=\"11\" y2=\"13\"/><polygon points=\"22 2 15 22 11 13 2 9 22 2\"/>
      </svg>
    </button>
  </div>
</div>
""")
f.close()
print("chat.component.html done")
