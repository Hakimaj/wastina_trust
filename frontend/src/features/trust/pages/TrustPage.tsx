import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  AsyncContent,
  Badge,
  Button,
  Card,
  CellText,
  DefinitionList,
} from '../../../components/primitives'
import { PageContainer } from '../../../components/layout/PageContainer'
import { useToast } from '../../../components/feedback/useToast'
import { queryKeys } from '../../../api/queryKeys'
import { useAuth } from '../../auth/AuthContext'
import {
  describeTrustError,
  getMyTrustScore,
  getMyTrustScoreHistory,
  recalculateMyTrustScore,
  scoreDelta,
  type TrustScore,
  type TrustScoreHistoryEntry,
} from '../api'
import { useI18n } from '../../../i18n/useI18n'

/**
 * The authenticated user's own trust score.
 *
 * Reads the stored snapshot (`GET`, read-only) and the score history, and offers an
 * explicit recalculation (`POST`). Loading this page never mutates a score — recalculation
 * is a deliberate action because it is a write.
 *
 * Only the caller's own score is shown. The backend permits administrators to read another
 * account's score, but that capability is intentionally not surfaced here.
 */
export function TrustPage() {
  const auth = useAuth()
  const { t } = useI18n()
  const { notify } = useToast()
  const queryClient = useQueryClient()
  const userId = auth.account?.id

  const scoreQuery = useQuery({
    queryKey: queryKeys.trust.mine(),
    queryFn: () => getMyTrustScore(),
    enabled: Boolean(userId),
  })
  const historyQuery = useQuery({
    queryKey: queryKeys.trust.history(50),
    queryFn: () => getMyTrustScoreHistory(50),
    enabled: Boolean(userId),
  })

  const recalculate = useMutation({
    mutationFn: () => recalculateMyTrustScore(),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.trust.all }),
      ])
      notify(t('trust.recalculated'), 'success')
    },
  })

  const score: TrustScore | null | undefined = scoreQuery.data

  return (
    <PageContainer title={t('trust.title')} description={t('trust.intro')}>
      {!userId ? (
        <Alert tone="danger">{auth.error ?? t('dashboard.loadError')}</Alert>
      ) : (
        <div className="feature-grid">
          <Card title={t('trust.currentScore')} className="feature-card">
            <AsyncContent
              isLoading={scoreQuery.isLoading}
              error={scoreQuery.error}
              data={score}
              isEmpty={(value) => value === null}
              emptyTitle={t('trust.noScoreTitle')}
              emptyDescription={t('trust.noScore')}
              onRetry={() => void scoreQuery.refetch()}
            >
              {(current) =>
                current ? (
                  <>
                    <p className="metric-value metric-value--large">
                      {current.score}
                      <span className="metric-unit">/100</span>
                    </p>
                    <DefinitionList
                      items={[
                        {
                          label: t('trust.confidence'),
                          value: <StatusToneLabel value={current.confidence} />,
                          key: 'confidence',
                        },
                        {
                          label: t('trust.evidence'),
                          value: <StatusToneLabel value={current.evidenceStatus} />,
                          key: 'evidence',
                        },
                        {
                          label: t('trust.policyVersion'),
                          value: <CellText value={current.policyVersion} />,
                          key: 'policy',
                        },
                        {
                          label: t('trust.calculatedAt'),
                          value: (
                            <CellText
                              value={
                                current.calculatedAt
                                  ? formatTimestamp(current.calculatedAt)
                                  : null
                              }
                            />
                          ),
                          key: 'calculatedAt',
                        },
                      ]}
                    />
                  </>
                ) : null
              }
            </AsyncContent>
          </Card>

          <Card title={t('trust.history')} className="feature-card feature-card--wide">
            <HistoryList
              entries={historyQuery.data}
              isLoading={historyQuery.isLoading}
              error={historyQuery.error}
              onRetry={() => void historyQuery.refetch()}
            />
          </Card>

          <div className="form-actions">
            <Button
              type="button"
              variant="secondary"
              loading={recalculate.isPending}
              onClick={() => recalculate.mutate()}
            >
              {recalculate.isPending
                ? t('trust.recalculating')
                : t('trust.recalculate')}
            </Button>
            {recalculate.isError ? (
              <Alert tone="danger">{describeTrustError(recalculate.error)}</Alert>
            ) : null}
          </div>
        </div>
      )}
    </PageContainer>
  )
}

function HistoryList({
  entries,
  isLoading,
  error,
  onRetry,
}: {
  entries: TrustScoreHistoryEntry[] | undefined
  isLoading: boolean
  error: unknown
  onRetry: () => void
}) {
  const { t } = useI18n()
  return (
    <AsyncContent
      isLoading={isLoading}
      error={error}
      data={entries}
      isEmpty={(rows) => rows.length === 0}
      emptyTitle={t('trust.noHistoryTitle')}
      emptyDescription={t('trust.noHistory')}
      onRetry={onRetry}
      loadingRows={4}
    >
      {(rows) => (
        <ol className="timeline">
          {rows.map((entry) => {
            const delta = scoreDelta(entry)
            return (
              <li className="timeline__entry" key={entry.id}>
                <span className="timeline__marker" aria-hidden="true" />
                <div className="timeline__body">
                  <div className="timeline__row">
                    <strong>
                      {entry.previousScore} → {entry.newScore}
                    </strong>
                    <Badge
                      tone={
                        delta > 0 ? 'success' : delta < 0 ? 'danger' : 'neutral'
                      }
                    >
                      {delta > 0 ? '+' : ''}
                      {delta}
                    </Badge>
                  </div>
                  <p className="timeline__meta">
                    <CellText value={entry.reasonCode} />
                    {' · '}
                    <CellText
                      value={
                        entry.createdAt
                          ? formatTimestamp(entry.createdAt)
                          : null
                      }
                    />
                  </p>
                  {entry.policyVersion ? (
                    <p className="timeline__meta">
                      {t('trust.policyVersion')}:{' '}
                      <CellText value={entry.policyVersion} />
                    </p>
                  ) : null}
                </div>
              </li>
            )
          })}
        </ol>
      )}
    </AsyncContent>
  )
}

/** Confidence and evidence labels carry their own tone. */
function StatusToneLabel({ value }: { value: string | null }) {
  if (!value) return <>—</>
  const tone =
    value.toUpperCase() === 'HIGH' || value.toUpperCase() === 'STRONG'
      ? 'success'
      : value.toUpperCase() === 'LOW' || value.toUpperCase() === 'WEAK'
        ? 'warning'
        : 'neutral'
  return <span className={`badge badge--${tone}`}>{value}</span>
}

function formatTimestamp(value: string): string {
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return value
  return parsed.toLocaleString()
}
