f = open("src/app/features/chat/chat.component.ts", "w", encoding="utf-8")
f.write("""import { Component, Input, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from \"@angular/core\";
import { CommonModule } from \"@angular/common\";
import { FormsModule } from \"@angular/forms\";
import { ChatService, ChatMessage } from \"../../services/chat.service\";

@Component({
  selector: \"app-chat\",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: \"./chat.component.html\",
  styleUrl: \"./chat.component.scss\"
})
export class ChatComponent implements OnDestroy, AfterViewChecked {
  @Input() section?: string;
  @ViewChild(\"messagesEnd\") messagesEnd!: ElementRef;

  inputText = \"\";
  showSources: { [idx: number]: boolean } = {};

  suggestions = [
    \"What are the key compliance risks?\",
    \"Summarize the customer answers\",
    \"Which RICS rules apply here?\"
  ];

  constructor(public chatService: ChatService) {}

  ngOnDestroy(): void { this.chatService.clearMessages(); }

  ngAfterViewChecked(): void {
    try { this.messagesEnd?.nativeElement.scrollIntoView({ behavior: \"smooth\" }); } catch {}
  }

  sendMessage(text?: string): void {
    const msg = (text || this.inputText).trim();
    if (!msg || this.chatService.loading()) return;
    this.inputText = \"\";
    this.chatService.sendMessage(msg, this.section);
  }

  onKeydown(e: KeyboardEvent): void {
    if (e.key === \"Enter\" && !e.shiftKey) { e.preventDefault(); this.sendMessage(); }
  }

  toggleSources(i: number): void { this.showSources[i] = !this.showSources[i]; }

  get messages(): ChatMessage[] { return this.chatService.messages(); }
  get isLoading(): boolean { return this.chatService.loading(); }

  formatAnswer(text: string): string {
    return text
      .replace(/\\*\\*(.*?)\\*\\*/g, \"<strong>$1</strong>\")
      .replace(/\\n- /g, \"<br>&bull; \")
      .replace(/\\n/g, \"<br>\");
  }
}
""")
f.close()
print("chat.component.ts done")
