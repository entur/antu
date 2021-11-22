/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package no.entur.antu.security;

import org.rutebanken.helper.organisation.AuthorizationConstants;
import org.rutebanken.helper.organisation.RoleAssignment;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorizationService {

    private final RoleAssignmentExtractor roleAssignmentExtractor;
    protected final boolean authorizationEnabled;

    public AuthorizationService(RoleAssignmentExtractor roleAssignmentExtractor, @Value("${authorization.enabled:true}") boolean authorizationEnabled) {
        this.roleAssignmentExtractor = roleAssignmentExtractor;
        this.authorizationEnabled = authorizationEnabled;
    }

    public void verifyAdministratorPrivileges() {
        verifyAtLeastOne(new AuthorizationClaim(AuthorizationConstants.ROLE_ROUTE_DATA_ADMIN));
    }

    /**
     * Users can edit route data if they have administrator privileges,
     * or if it has editor privileges for this provider.
     *
     * @param codespace
     */
    public void verifyRouteDataEditorPrivileges(String codespace) {
        verifyAtLeastOne(new AuthorizationClaim(AuthorizationConstants.ROLE_ROUTE_DATA_ADMIN),
                new AuthorizationClaim(AuthorizationConstants.ROLE_ROUTE_DATA_EDIT, codespace));
    }

    protected void verifyAtLeastOne(AuthorizationClaim... claims) {
        if (!authorizationEnabled) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<RoleAssignment> roleAssignments = roleAssignmentExtractor.getRoleAssignmentsForUser(authentication);

        boolean authorized = false;
        for (AuthorizationClaim claim : claims) {
            if (claim.getCodespace() == null) {
                authorized |= roleAssignments.stream().anyMatch(ra -> claim.getRequiredRole().equals(ra.getRole()));
            } else {
                authorized |= hasRoleForCodespace(roleAssignments, claim);
            }
        }

        if (!authorized) {
            throw new AccessDeniedException("Insufficient privileges for operation");
        }

    }

    private boolean hasRoleForCodespace(List<RoleAssignment> roleAssignments, AuthorizationClaim claim) {
        return roleAssignments.stream().anyMatch(roleAssignment -> claim.getCodespace().equals(roleAssignment.getOrganisation()) && claim.getRequiredRole().equals(roleAssignment.getRole()));
    }

}
