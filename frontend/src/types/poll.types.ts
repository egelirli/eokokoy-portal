export type PollType = 'vote' | 'survey';
export type PollStatus = 'draft' | 'active' | 'closed' | 'cancelled';
export type QuestionType = 'yes_no' | 'single_choice' | 'multiple_choice' | 'text';

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

export interface PollOption {
  id: string;
  optionText: string;
  optionOrder: number;
}

export interface PollQuestion {
  id: string;
  questionText: string;
  questionType: QuestionType;
  isRequired: boolean;
  questionOrder: number;
  options: PollOption[];
}

export interface PollDetail extends Poll {
  questions: PollQuestion[];
}

export interface PollAnswer {
  questionId: string;
  optionId?: string;
  optionIds?: string[];
  textAnswer?: string;
}

export interface PollRespondRequest {
  answers: PollAnswer[];
}
