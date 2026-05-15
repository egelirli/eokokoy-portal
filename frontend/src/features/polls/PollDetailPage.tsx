import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { getPollById, respondToPoll } from '@/api/polls';
import { Spinner, ErrorState } from '@/components/common/QueryStates';
import type { PollStatus, PollQuestion, PollAnswer } from '@/types/poll.types';

const STATUS_LABEL: Record<PollStatus, string> = {
  draft: 'Taslak',
  active: 'Aktif',
  closed: 'Kapandı',
  cancelled: 'İptal',
};

const STATUS_BADGE: Record<PollStatus, string> = {
  draft: 'bg-muted text-muted-foreground',
  active: 'bg-primary/10 text-primary',
  closed: 'bg-muted text-muted-foreground',
  cancelled: 'bg-destructive/10 text-destructive',
};

function QuestionView({
  question,
  answer,
  onChange,
  readonly,
}: {
  question: PollQuestion;
  answer: PollAnswer;
  onChange: (a: PollAnswer) => void;
  readonly: boolean;
}) {
  const { questionType, options } = question;

  if (questionType === 'yes_no' || questionType === 'single_choice') {
    return (
      <div className="space-y-2">
        {options.map(opt => (
          <label
            key={opt.id}
            className={cn(
              'flex items-center gap-3 rounded-lg border p-3 cursor-pointer transition-colors',
              answer.optionId === opt.id
                ? 'border-primary bg-primary/5'
                : 'border-border bg-background-card hover:border-primary/30',
              readonly && 'cursor-default pointer-events-none',
            )}
          >
            <input
              type="radio"
              name={question.id}
              value={opt.id}
              checked={answer.optionId === opt.id}
              onChange={() => onChange({ questionId: question.id, optionId: opt.id })}
              disabled={readonly}
              className="accent-primary"
            />
            <span className="text-sm text-foreground">{opt.optionText}</span>
          </label>
        ))}
      </div>
    );
  }

  if (questionType === 'multiple_choice') {
    const selectedIds = answer.optionIds ?? [];
    return (
      <div className="space-y-2">
        {options.map(opt => (
          <label
            key={opt.id}
            className={cn(
              'flex items-center gap-3 rounded-lg border p-3 cursor-pointer transition-colors',
              selectedIds.includes(opt.id)
                ? 'border-primary bg-primary/5'
                : 'border-border bg-background-card hover:border-primary/30',
              readonly && 'cursor-default pointer-events-none',
            )}
          >
            <input
              type="checkbox"
              value={opt.id}
              checked={selectedIds.includes(opt.id)}
              onChange={e => {
                const next = e.target.checked
                  ? [...selectedIds, opt.id]
                  : selectedIds.filter(id => id !== opt.id);
                onChange({ questionId: question.id, optionIds: next });
              }}
              disabled={readonly}
              className="accent-primary"
            />
            <span className="text-sm text-foreground">{opt.optionText}</span>
          </label>
        ))}
      </div>
    );
  }

  if (questionType === 'text') {
    return (
      <textarea
        rows={3}
        disabled={readonly}
        value={answer.textAnswer ?? ''}
        onChange={e => onChange({ questionId: question.id, textAnswer: e.target.value })}
        className="w-full rounded-lg border border-border bg-background-card px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none disabled:opacity-60"
        placeholder="Yanıtınızı buraya yazın…"
      />
    );
  }

  return null;
}

export function PollDetailPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['polls', id],
    queryFn: () => getPollById(id!),
    enabled: !!id,
    retry: 1,
  });

  const [answers, setAnswers] = useState<Record<string, PollAnswer>>({});
  const [submitted, setSubmitted] = useState(false);

  const respond = useMutation({
    mutationFn: (req: Parameters<typeof respondToPoll>[1]) => respondToPoll(id!, req),
    onSuccess: () => {
      setSubmitted(true);
      queryClient.invalidateQueries({ queryKey: ['polls'] });
    },
  });

  const isActive = data?.status === 'active';
  const canRespond = isActive && !data?.hasResponded && !submitted;

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!data) return;

    const answerList = data.questions.map(q => answers[q.id] ?? { questionId: q.id });
    respond.mutate({ answers: answerList });
  }

  function updateAnswer(a: PollAnswer) {
    setAnswers(prev => ({ ...prev, [a.questionId]: a }));
  }

  const endsAt = data?.endsAt
    ? new Date(data.endsAt).toLocaleDateString('tr-TR', { day: 'numeric', month: 'long', year: 'numeric' })
    : null;

  return (
    <div className="p-6 max-w-2xl space-y-6">
      <Link
        to="/polls"
        className="inline-flex items-center gap-1.5 text-sm font-semibold text-primary hover:underline"
      >
        ← Anketlere dön
      </Link>

      {isLoading && <Spinner />}

      {isError && <ErrorState onRetry={() => refetch()} />}

      {data && (
        <>
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-muted px-2.5 py-0.5 text-[11px] font-bold text-muted-foreground">
                {data.type === 'vote' ? 'Oylama' : 'Anket'}
              </span>
              <span className={cn('rounded-full px-2.5 py-0.5 text-[11px] font-bold', STATUS_BADGE[data.status])}>
                {STATUS_LABEL[data.status]}
              </span>
              {data.isAnonymous && (
                <span className="rounded-full bg-muted px-2.5 py-0.5 text-[11px] font-semibold text-muted-foreground">
                  Anonim
                </span>
              )}
            </div>

            <h1 className="text-xl font-bold text-foreground leading-snug">{data.title}</h1>

            {data.description && (
              <p className="text-sm text-muted-foreground leading-relaxed">{data.description}</p>
            )}

            <div className="flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
              <span>{data.questions.length} soru</span>
              {endsAt && <><span>·</span><span>Bitiş: {endsAt}</span></>}
            </div>
          </div>

          {(data.hasResponded || submitted) && (
            <div className="rounded-xl border border-primary/30 bg-primary/5 px-4 py-3">
              <p className="text-sm font-semibold text-primary">✓ Bu ankete yanıt verdiniz.</p>
            </div>
          )}

          {!isActive && data.status === 'closed' && (
            <div className="rounded-xl border border-border bg-background-card px-4 py-3">
              <p className="text-sm text-muted-foreground">Bu oylama sona erdi. Sonuçlar için yöneticiye başvurun.</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            {data.questions
              .sort((a, b) => a.questionOrder - b.questionOrder)
              .map((q, idx) => (
                <div key={q.id} className="space-y-3">
                  <div className="flex items-start gap-2">
                    <span className="flex-shrink-0 rounded-full bg-primary/10 px-2 py-0.5 text-xs font-bold text-primary">
                      {idx + 1}
                    </span>
                    <p className="text-sm font-semibold text-foreground leading-snug">
                      {q.questionText}
                      {q.isRequired && <span className="ml-1 text-destructive">*</span>}
                    </p>
                  </div>
                  <QuestionView
                    question={q}
                    answer={answers[q.id] ?? { questionId: q.id }}
                    onChange={updateAnswer}
                    readonly={!canRespond}
                  />
                </div>
              ))}

            {canRespond && (
              <div className="pt-2">
                {respond.isError && (
                  <p className="mb-3 text-sm text-destructive">
                    Yanıtınız kaydedilemedi. Lütfen tekrar deneyin.
                  </p>
                )}
                <button
                  type="submit"
                  disabled={respond.isPending}
                  className="rounded-lg bg-primary px-6 py-2.5 text-sm font-bold text-primary-foreground hover:opacity-90 disabled:opacity-60 transition-opacity"
                >
                  {respond.isPending ? 'Gönderiliyor…' : 'Yanıtla'}
                </button>
              </div>
            )}
          </form>
        </>
      )}
    </div>
  );
}
