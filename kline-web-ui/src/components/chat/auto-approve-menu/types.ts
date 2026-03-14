import { AutoApprovalSettings } from "@/shared/AutoApprovalSettings.ts"

export interface ActionMetadata {
	id: keyof AutoApprovalSettings["actions"] | "enableNotifications" | "enableAll" | "enableAutoApprove"
	label: string
	shortName: string
	description: string
	icon: string
	subAction?: ActionMetadata
	sub?: boolean
	parentActionId?: string
}
