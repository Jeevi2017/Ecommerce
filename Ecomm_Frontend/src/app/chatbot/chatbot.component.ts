import { Component, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChatbotService } from './chatbot.service';

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chatbot.component.html',
  styleUrls: ['./chatbot.component.css']
})
export class ChatbotComponent implements AfterViewChecked {

  isOpen = false;
  messages: { sender: string; text: string }[] = [];

  @ViewChild('chatBody') chatBody!: ElementRef;

  // ⭐ this flag tells Angular "a new message arrived"
  private pendingScroll = false;

  constructor(private botService: ChatbotService) {}

  /* OPEN / CLOSE CHAT */
  toggleChat() {
    this.isOpen = !this.isOpen;
    this.pendingScroll = true;
  }

  /* SEND MESSAGE */
  sendMessage(input: HTMLInputElement) {

    const text = input.value.trim();
    if (!text) return;

    // user message
    this.messages.push({ sender: 'user', text });
    input.value = '';

    this.pendingScroll = true;

    // bot reply
    this.botService.getResponse(text).then(reply => {

      setTimeout(() => {
        this.messages.push({ sender: 'bot', text: reply });
        this.pendingScroll = true;
      }, 400);

    }).catch(() => {
      this.messages.push({
        sender: 'bot',
        text: 'Something went wrong. Please try again.'
      });
      this.pendingScroll = true;
    });
  }

  /* ⭐ THIS IS THE IMPORTANT PART ⭐
     Runs AFTER Angular updates the DOM */
  ngAfterViewChecked() {
    if (this.pendingScroll) {
      this.scrollToBottom();
      this.pendingScroll = false;
    }
  }

  private scrollToBottom() {
    try {
      const el = this.chatBody.nativeElement;
      el.scrollTop = el.scrollHeight;
    } catch {}
  }
}