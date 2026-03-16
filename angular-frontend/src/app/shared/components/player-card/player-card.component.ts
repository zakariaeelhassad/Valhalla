import { Component, Input } from '@angular/core';
import { PlayerSummary } from '../../../core/services/api.service';

const POS_STYLES: Record<string, { bg: string; border: string; text: string }> = {
  GK: { bg: 'rgba(251,191,36,0.15)', border: 'rgba(251,191,36,0.4)', text: '#fbbf24' },
  DEF: { bg: 'rgba(56,189,248,0.15)', border: 'rgba(56,189,248,0.4)', text: '#38bdf8' },
  MID: { bg: 'rgba(74,222,128,0.15)', border: 'rgba(74,222,128,0.4)', text: '#4ade80' },
  FWD: { bg: 'rgba(248,113,113,0.15)', border: 'rgba(248,113,113,0.4)', text: '#f87171' },
};

@Component({
  selector: 'app-player-card',
  standalone: true,
  imports: [],
  template: `
  <div class="relative flex flex-col items-center bg-white/[0.04] border border-white/[0.08] rounded-2xl
              px-2.5 py-3.5 cursor-default transition-all duration-200 min-w-[86px] max-w-[116px]
              hover:bg-white/[0.08] hover:border-sky-400/30 hover:-translate-y-1 hover:shadow-card">

    <!-- Captain / VC badges -->
    @if (isCaptain) {
      <span class="absolute -top-2 -right-2 w-5 h-5 rounded-full bg-amber-400 text-[#1a1000]
                   flex items-center justify-center text-xs font-black z-10 shadow-[0_0_8px_rgba(251,191,36,0.6)]">©</span>
    }
    @if (isViceCaptain && !isCaptain) {
      <span class="absolute -top-2 -right-2 w-5 h-5 rounded-full bg-indigo-400 text-white
                   flex items-center justify-center text-xs font-black z-10 shadow-[0_0_8px_rgba(129,140,248,0.6)]">V</span>
    }

    <!-- Avatar -->
    <div class="relative w-11 h-11 rounded-full flex items-center justify-center mb-2 flex-shrink-0"
         [style.background]="'linear-gradient(135deg,' + posStyle.border + ',' + posStyle.bg + ')'">
      <span class="text-sm font-bold text-white">{{ initial }}</span>
      <div class="absolute inset-[-3px] rounded-full border-2 opacity-60"
           [style.borderColor]="posStyle.border"></div>
    </div>

    <!-- Info -->
    <div class="flex flex-col items-center gap-0.5 w-full">
      <span class="text-xs font-semibold text-slate-100 text-center leading-tight break-words">{{ player.name || 'Unknown' }}</span>
      <span class="text-[10px] text-slate-500 text-center">{{ player.realTeam || '—' }}</span>
    </div>

    <!-- Footer row -->
    <div class="flex items-center justify-between w-full mt-2.5 gap-1">
      <span class="text-[9px] font-bold px-1.5 py-0.5 rounded"
            [style.background]="posStyle.bg"
            [style.color]="posStyle.text"
            [style.border]="'1px solid ' + posStyle.border">
        {{ pos }}
      </span>
      <span class="text-xs font-bold text-slate-100">
        {{ player.totalPoints }}<small class="text-[9px] text-slate-500 font-normal"> pts</small>
      </span>
    </div>
  </div>
  `
})
export class PlayerCardComponent {
  @Input() player!: PlayerSummary;
  @Input() isCaptain = false;
  @Input() isViceCaptain = false;

  get pos(): string { return (this.player.position || 'MID').toUpperCase(); }
  get posStyle() { return POS_STYLES[this.pos] || POS_STYLES['MID']; }
  get initial(): string {
    return (this.player.name || '?').split(' ').map((w: string) => w[0]).join('').slice(0, 2).toUpperCase();
  }
}

