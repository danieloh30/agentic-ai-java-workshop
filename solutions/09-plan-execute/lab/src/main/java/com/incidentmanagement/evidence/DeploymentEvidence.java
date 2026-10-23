package com.incidentmanagement.evidence;

/** Fixed workshop evidence, never a claim about a real deployment. */
public record DeploymentEvidence(String service, String status, String deployment, String observation) {
    public boolean usableFor(String requestedService) {
        return requestedService.equals(service) && "available".equals(status)
                && deployment != null && !deployment.isBlank()
                && observation != null && !observation.isBlank();
    }
}
