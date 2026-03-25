import { PlayerSummary } from '../../../core/models';

export type PointsPlayer = PlayerSummary & { gameweekPoints: number; starter?: boolean };

export type PitchSquad = {
  gks: PointsPlayer[];
  defs: PointsPlayer[];
  mids: PointsPlayer[];
  fwds: PointsPlayer[];
};
