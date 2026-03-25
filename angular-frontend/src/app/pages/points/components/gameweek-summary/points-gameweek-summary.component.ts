import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-points-gameweek-summary',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './points-gameweek-summary.component.html'
})
export class PointsGameweekSummaryComponent {
  @Input() shouldShow = false;
  @Input() viewedGameweek = 1;
  @Input() gwPoints = 0;
  @Input() highestPoints = 0;

  @Output() gameweekChange = new EventEmitter<number>();

  changeGameweek(delta: number): void {
    this.gameweekChange.emit(delta);
  }
}
