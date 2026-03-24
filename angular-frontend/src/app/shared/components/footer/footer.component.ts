import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
    selector: 'app-footer',
    standalone: true,
    imports: [RouterLink],
    template: `
  <footer class="bg-[#0d1220] border-t border-white/[0.08] pt-14 pb-8 mt-auto">
    <div class="max-w-7xl mx-auto px-6">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-10 mb-10">

        <!-- Brand -->
        <div>
          <div class="flex items-center gap-2 mb-3">
            <span class="text-xl">⚽</span>
            <span class="font-outfit font-extrabold text-lg text-slate-100">
              <span class="gradient-text">Valhalla</span>
            </span>
          </div>
          <p class="text-slate-500 text-sm">The Moroccan Fantasy Football Experience.</p>
        </div>

        <!-- Play links -->
        <div class="flex flex-col gap-2">
          <span class="text-xs font-bold tracking-widest uppercase text-slate-500 mb-1">Play</span>
          <a routerLink="/"          class="text-slate-400 text-sm hover:text-sky-400 transition-colors no-underline">Home</a>
          <a routerLink="/register"  class="text-slate-400 text-sm hover:text-sky-400 transition-colors no-underline">Register</a>
          <a routerLink="/login"     class="text-slate-400 text-sm hover:text-sky-400 transition-colors no-underline">Log In</a>
          <a routerLink="/dashboard" class="text-slate-400 text-sm hover:text-sky-400 transition-colors no-underline">Dashboard</a>
        </div>

        <!-- Rules -->
        <div class="flex flex-col gap-2">
          <span class="text-xs font-bold tracking-widest uppercase text-slate-500 mb-1">Rules</span>
          <span class="text-slate-400 text-sm">15-player squads</span>
          <span class="text-slate-400 text-sm">Budget: 100.0</span>
          <span class="text-slate-400 text-sm">GK, DEF, MID, FWD</span>
          <span class="text-slate-400 text-sm">Point-based scoring</span>
        </div>
      </div>

      <div class="border-t border-white/[0.05] pt-6 flex flex-wrap items-center justify-between gap-2">
        <span class="text-slate-500 text-xs">© 2026 Valhalla. All rights reserved.</span>
        <span class="text-slate-500 text-xs">Built with ❤️ for football fans</span>
      </div>
    </div>
  </footer>
  `
})
export class FooterComponent { }
