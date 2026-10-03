import type { ReactNode } from 'react'

export type DataTableColumn<Row> = {
  key: string
  header: ReactNode
  render?: (row: Row) => ReactNode
  rowHeader?: boolean
}

export type DataTableProps<Row> = {
  caption: string
  columns: DataTableColumn<Row>[]
  rows: Row[]
  getRowKey: (row: Row) => string | number
  emptyState?: ReactNode
  className?: string
}

export function DataTable<Row>({
  caption,
  columns,
  rows,
  getRowKey,
  emptyState = 'No records found.',
  className = '',
}: DataTableProps<Row>) {
  return (
    <div className="data-table__scroll" role="region" aria-label={caption} tabIndex={0}>
      <table className={['data-table', className].filter(Boolean).join(' ')}>
        <caption className="visually-hidden">{caption}</caption>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key} scope="col">{column.header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length > 0 ? rows.map((row) => (
            <tr key={getRowKey(row)}>
              {columns.map((column) => {
                const content = column.render ? column.render(row) : String(row[column.key as keyof Row] ?? '')
                return column.rowHeader ? (
                  <th key={column.key} scope="row">{content}</th>
                ) : (
                  <td key={column.key}>{content}</td>
                )
              })}
            </tr>
          )) : (
            <tr>
              <td className="data-table__empty" colSpan={Math.max(columns.length, 1)}>{emptyState}</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
