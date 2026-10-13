import { z } from 'zod'

export const foundationContactSchema = z.object({
  email: z.string().email(),
  phoneNumber: z.string().min(1),
})

export type FoundationContactValues = z.infer<typeof foundationContactSchema>

export const workerProfileSchema = z.object({
  professionName: z.string().trim().min(1),
  locationName: z.string().trim().min(1),
  isAvailable: z.boolean(),
})

export const employerProfileSchema = z.object({
  companyName: z.string().trim().min(1),
  locationName: z.string().trim().min(1),
  subscriptionTier: z.string().trim().min(1),
})

export const guarantorProfileSchema = z.object({
  financialLimit: z.number().nonnegative().nullable(),
  employerName: z.string().trim().min(1),
})

export const employmentCreateSchema = z.object({
  organizationId: z.string().uuid(),
  jobTitleEn: z.string().trim().min(1),
  jobTitleAm: z.string().optional(),
  occupationCode: z.string().optional(),
  department: z.string().optional(),
  location: z.string().optional(),
  employmentType: z.enum([
    'FULL_TIME', 'PART_TIME', 'TEMPORARY', 'CONTRACT', 'CASUAL',
    'INTERNSHIP', 'APPRENTICESHIP', 'CONSULTANT', 'SEASONAL', 'OTHER',
  ]),
  workRelationshipType: z.enum([
    'EMPLOYEE', 'CONTRACTOR', 'CONSULTANT', 'SELF_EMPLOYED', 'VOLUNTEER', 'INTERN', 'OTHER',
  ]),
  sector: z.enum(['PUBLIC', 'PRIVATE', 'NGO', 'OTHER']),
  startDate: z.string().min(1),
  endDate: z.string().optional(),
  currentEmployment: z.boolean(),
  description: z.string().optional(),
}).superRefine((values, context) => {
  if (!values.currentEmployment && !values.endDate) {
    context.addIssue({ code: 'custom', path: ['endDate'], message: 'An end date is required for past employment.' })
  }
  if (values.endDate && values.endDate < values.startDate) {
    context.addIssue({ code: 'custom', path: ['endDate'], message: 'End date must be on or after the start date.' })
  }
})

export type WorkerProfileValues = z.infer<typeof workerProfileSchema>
export type EmployerProfileValues = z.infer<typeof employerProfileSchema>
export type GuarantorProfileValues = z.infer<typeof guarantorProfileSchema>
export type EmploymentCreateValues = z.infer<typeof employmentCreateSchema>
