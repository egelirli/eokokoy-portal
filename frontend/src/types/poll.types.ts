export type PollType = 'vote' | 'survey';
export type PollStatus = 'draft' | 'active' | 'closed' | 'cancelled';

export interface Poll {
  id: string;
  type: PollType;
  title: string;
  description: string | null;
  status: PollStatus;
  isAnonymous: boolean;
  eligibleRoles: string[];
  startsAt: string;
  endsAt: string | null;
  questionCount: number;
  hasResponded: boolean;
  createdAt: string;
}
