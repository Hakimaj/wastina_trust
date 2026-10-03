import { z } from 'zod'

export const foundationContactSchema = z.object({
  email: z.string().email(),
  phoneNumber: z.string().min(1),
})

export type FoundationContactValues = z.infer<typeof foundationContactSchema>
