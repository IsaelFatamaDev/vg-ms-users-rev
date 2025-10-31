package pe.edu.vallegrande.vgmsusers.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class OrganizationClient {

    private final WebClient webClient;

    @Value("${external.ms-organizations.url}")
    private String organizationServiceUrl;

    public OrganizationClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<OrganizationResponse> getOrganizationById(String organizationId) {
        return webClient.get()
                .uri(organizationServiceUrl + "/organization/" + organizationId)
                .retrieve()
                .bodyToMono(OrganizationResponse.class)
                .onErrorReturn(new OrganizationResponse(false, null));
    }

    public static class OrganizationResponse {
        private boolean status;
        private OrganizationData data;

        public OrganizationResponse() {
        }

        public OrganizationResponse(boolean status, OrganizationData data) {
            this.status = status;
            this.data = data;
        }

        public boolean isStatus() {
            return status;
        }

        public void setStatus(boolean status) {
            this.status = status;
        }

        public OrganizationData getData() {
            return data;
        }

        public void setData(OrganizationData data) {
            this.data = data;
        }
    }

    public static class OrganizationData {
        @JsonProperty("organizationId")
        private String organizationId;

        @JsonProperty("organizationCode")
        private String organizationCode;

        @JsonProperty("organizationName")
        private String organizationName;

        @JsonProperty("legalRepresentative")
        private String legalRepresentative;

        @JsonProperty("address")
        private String address;

        @JsonProperty("phone")
        private String phone;

        @JsonProperty("status")
        private String status;

        @JsonProperty("logo")
        private String logo;

        @JsonProperty("zones")
        private List<Zone> zones;

        public String getOrganizationId() {
            return organizationId;
        }

        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }

        public String getOrganizationCode() {
            return organizationCode;
        }

        public void setOrganizationCode(String organizationCode) {
            this.organizationCode = organizationCode;
        }

        public String getOrganizationName() {
            return organizationName;
        }

        public void setOrganizationName(String organizationName) {
            this.organizationName = organizationName;
        }

        public String getLegalRepresentative() {
            return legalRepresentative;
        }

        public void setLegalRepresentative(String legalRepresentative) {
            this.legalRepresentative = legalRepresentative;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getLogo() {
            return logo;
        }

        public void setLogo(String logo) {
            this.logo = logo;
        }

        public List<Zone> getZones() {
            return zones;
        }

        public void setZones(List<Zone> zones) {
            this.zones = zones;
        }
    }

    public static class Zone {
        @JsonProperty("zoneId")
        private String zoneId;

        @JsonProperty("organizationId")
        private String organizationId;

        @JsonProperty("zoneCode")
        private String zoneCode;

        @JsonProperty("zoneName")
        private String zoneName;

        @JsonProperty("description")
        private String description;

        @JsonProperty("status")
        private String status;

        @JsonProperty("streets")
        private List<Street> streets;

        // Getters y Setters
        public String getZoneId() {
            return zoneId;
        }

        public void setZoneId(String zoneId) {
            this.zoneId = zoneId;
        }

        public String getOrganizationId() {
            return organizationId;
        }

        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }

        public String getZoneCode() {
            return zoneCode;
        }

        public void setZoneCode(String zoneCode) {
            this.zoneCode = zoneCode;
        }

        public String getZoneName() {
            return zoneName;
        }

        public void setZoneName(String zoneName) {
            this.zoneName = zoneName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<Street> getStreets() {
            return streets;
        }

        public void setStreets(List<Street> streets) {
            this.streets = streets;
        }
    }

    public static class Street {
        @JsonProperty("streetId")
        private String streetId;

        @JsonProperty("zoneId")
        private String zoneId;

        @JsonProperty("streetCode")
        private String streetCode;

        @JsonProperty("streetName")
        private String streetName;

        @JsonProperty("streetType")
        private String streetType;

        @JsonProperty("status")
        private String status;

        @JsonProperty("createdAt")
        private String createdAt;

        // Getters y Setters
        public String getStreetId() {
            return streetId;
        }

        public void setStreetId(String streetId) {
            this.streetId = streetId;
        }

        public String getZoneId() {
            return zoneId;
        }

        public void setZoneId(String zoneId) {
            this.zoneId = zoneId;
        }

        public String getStreetCode() {
            return streetCode;
        }

        public void setStreetCode(String streetCode) {
            this.streetCode = streetCode;
        }

        public String getStreetName() {
            return streetName;
        }

        public void setStreetName(String streetName) {
            this.streetName = streetName;
        }

        public String getStreetType() {
            return streetType;
        }

        public void setStreetType(String streetType) {
            this.streetType = streetType;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }
}
