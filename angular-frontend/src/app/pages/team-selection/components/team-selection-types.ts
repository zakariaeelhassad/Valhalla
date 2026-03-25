import { PlayerSummary } from '../../../core/models';

export interface PitchSlot {
  id: number;
  position: 'GK' | 'DEF' | 'MID' | 'FWD';
  player: PlayerSummary | null;
}
