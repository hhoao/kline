import { stateService } from "@/api/state"
import { AutoApprovalSettings } from "@/shared/AutoApprovalSettings"

/**
 * Updates auto approval settings using the gRPC/Protobus client
 * @param settings The auto approval settings to update
 * @throws Error if the update fails
 */
export async function updateAutoApproveSettings(settings: AutoApprovalSettings): Promise<void> {
	try {
		await stateService.updateAutoApprovalSettings(settings)
	} catch (error) {
		console.error("Failed to update auto approval settings:", error)
		throw error
	}
}
