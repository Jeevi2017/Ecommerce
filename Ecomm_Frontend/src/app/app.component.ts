import { Component, OnInit } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ChatbotComponent } from './chatbot/chatbot.component';
import { ChatbotService } from './chatbot/chatbot.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule, ChatbotComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  showChatbot = false;

  constructor(private chatbotService: ChatbotService,
              private router: Router) {}

  ngOnInit() {

    // check when app loads
    this.updateChatbotVisibility();

    // check after login/logout navigation
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.updateChatbotVisibility();
      });
  }

  updateChatbotVisibility() {
    this.showChatbot = this.chatbotService.isCustomer();
  }
}