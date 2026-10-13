import { zodResolver } from '@hookform/resolvers/zod'
import {
  useForm,
  type DefaultValues,
  type FieldValues,
  type SubmitHandler,
  type UseFormReturn,
} from 'react-hook-form'
import type { z } from 'zod'

/**
 * Binds a Zod schema to React Hook Form.
 *
 * React Hook Form types a form on a single shape, while Zod distinguishes the shape a developer
 * writes (`input`) from the shape produced after parsing (`output`). Pinning both to the same type
 * keeps the resolver's generics aligned; for schemas that only validate and coerce, the two are
 * identical, and the submit handler then receives values that have already been parsed.
 */
export function useFoundationForm<TValues extends FieldValues>(
  schema: z.ZodType<TValues, TValues>,
  defaults?: DefaultValues<TValues>,
): UseFormReturn<TValues> {
  return useForm<TValues, unknown, TValues>({
    resolver: zodResolver(schema),
    defaultValues: defaults,
  })
}

export type FoundationSubmit<T extends FieldValues> = SubmitHandler<T>