package com.standardinsurance.itsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ItSupportIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    private String token(String userId, String password) throws Exception {
        String body = "{\"userId\":\"" + userId + "\",\"password\":\"" + password + "\"}";
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String bearer(String userId, String pw) throws Exception {
        return "Bearer " + token(userId, pw);
    }

    private long createTicket(String auth, String title, String category) throws Exception {
        String body = "{\"title\":\"" + title + "\",\"description\":\"d\",\"categoryCode\":\""
                + category + "\"}";
        MvcResult res = mvc.perform(post("/api/tickets").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    // ---- Login ----

    @Test
    void allSeededUsersCanLogin() throws Exception {
        for (String[] u : new String[][]{
                {"1001", "Admin"}, {"1002", "Leiva"}, {"1003", "Rudy"},
                {"1004", "Rich"}, {"1005", "Paw"}}) {
            assertThat(token(u[0], u[1])).isNotBlank();
        }
    }

    @Test
    void adminHasAdminRole() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"1001\",\"password\":\"Admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ROLE_ADMIN"));
    }

    @Test
    void badPasswordReturns401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"1003\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void protectedEndpointRequiresToken() throws Exception {
        mvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    // ---- Ticket lifecycle ----

    @Test
    void fullApprovalLifecycleWithAuditAndEmail() throws Exception {
        String requestor = bearer("1002", "Leiva");
        String approver = bearer("1004", "Rich");
        // clear outbox first
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/dev/outbox"));

        long id = createTicket(requestor, "Email lifecycle", "SR");

        mvc.perform(post("/api/tickets/" + id + "/submit").header("Authorization", requestor))
                .andExpect(jsonPath("$.status").value("For Approval"));
        mvc.perform(post("/api/tickets/" + id + "/approve").header("Authorization", approver))
                .andExpect(jsonPath("$.status").value("In Process"))
                .andExpect(jsonPath("$.approverId").value("1004"));
        mvc.perform(post("/api/tickets/" + id + "/resolve").header("Authorization", approver))
                .andExpect(jsonPath("$.status").value("Done/Resolved"));
        mvc.perform(post("/api/tickets/" + id + "/close").header("Authorization", requestor))
                .andExpect(jsonPath("$.status").value("Closed"));

        // Audit: created + submitted + approved + resolved + closed = 5 status rows
        mvc.perform(get("/api/tickets/" + id + "/audit").header("Authorization", requestor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].action").value("TICKET_CREATED"))
                .andExpect(jsonPath("$[4].action").value("TICKET_CLOSED"));

        // Email outbox received notifications for this lifecycle
        mvc.perform(get("/api/dev/outbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(4)));
    }

    @Test
    void requestorCannotApproveOwnTicket() throws Exception {
        String requestor = bearer("1002", "Leiva");
        long id = createTicket(requestor, "Self approve", "SR");
        mvc.perform(post("/api/tickets/" + id + "/submit").header("Authorization", requestor));
        mvc.perform(post("/api/tickets/" + id + "/approve").header("Authorization", requestor))
                .andExpect(status().isForbidden());
    }

    @Test
    void illegalTransitionReturns409() throws Exception {
        String requestor = bearer("1002", "Leiva");
        long id = createTicket(requestor, "Bad transition", "SR");
        // cannot close a NEW ticket
        mvc.perform(post("/api/tickets/" + id + "/close").header("Authorization", requestor))
                .andExpect(status().isConflict());
    }

    // ---- Restriction ----

    @Test
    void restrictedUserCannotCreateDbTicket() throws Exception {
        String rudy = bearer("1003", "Rudy");
        String body = "{\"title\":\"db\",\"description\":\"d\",\"categoryCode\":\"DB\"}";
        mvc.perform(post("/api/tickets").header("Authorization", rudy)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User 1003 is restricted from category DB"));
    }

    // ---- Admin security & maintenance ----

    @Test
    void nonAdminForbiddenFromAdminApi() throws Exception {
        mvc.perform(get("/api/admin/users").header("Authorization", bearer("1002", "Leiva")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListUsersAndSeededRestrictionPresent() throws Exception {
        mvc.perform(get("/api/admin/users/1003/restrictions")
                        .header("Authorization", bearer("1001", "Admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("DB"));
    }

    // ---- Dashboard ----

    @Test
    void dashboardReturnsAllCategoriesAndStatuses() throws Exception {
        mvc.perform(get("/api/dashboard/summary").header("Authorization", bearer("1001", "Admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byCategory.length()").value(5))
                .andExpect(jsonPath("$.byStatus.length()").value(7));
    }

    // ---- Report ----

    @Test
    void csvReportDownloads() throws Exception {
        mvc.perform(get("/api/reports/tickets?format=csv")
                        .header("Authorization", bearer("1001", "Admin")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString(".csv")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    // ---- Specs ----

    @Test
    void specsListAndReadWork() throws Exception {
        String auth = bearer("1001", "Admin");
        mvc.perform(get("/api/specs").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
        mvc.perform(get("/api/specs/00-overview.md").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markdown", org.hamcrest.Matchers.containsString("#")));
    }

    @Test
    void specsPathTraversalRejected() throws Exception {
        mvc.perform(get("/api/specs/..%2f..%2fCLAUDE.md")
                        .header("Authorization", bearer("1001", "Admin")))
                .andExpect(status().isBadRequest());
    }
}
