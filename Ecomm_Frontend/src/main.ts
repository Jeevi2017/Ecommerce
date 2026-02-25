/// <reference types="@angular/localize" />

import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';

// ⭐ IMPORTANT
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

bootstrapApplication(AppComponent, {
  ...appConfig,
  providers: [
    ...(appConfig.providers || []),

    // ⭐ THIS registers Chart.js globally in Angular
    provideCharts(withDefaultRegisterables())
  ]
}).catch(err => console.error(err));
