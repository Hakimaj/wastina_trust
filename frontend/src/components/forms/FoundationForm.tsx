import { zodResolver } from '@hookform/resolvers/zod'
import {
  useForm,
  type DefaultValues,
  type FieldValues,
  type SubmitHandler,
  type UseFormReturn,
} from 'react-hook-form'
import type { z } from 'zod'

export function useFoundationForm<TSchema extends z.ZodType<FieldValues>>(
  schema: TSchema,
  defaults?: DefaultValues<z.infer<TSchema>>,
): UseFormReturn<z.infer<TSchema>> {
  return useForm<z.infer<TSchema>>({
    resolver: zodResolver(schema),
    defaultValues: defaults,
  })
}

export type FoundationSubmit<T extends FieldValues> = SubmitHandler<T>
