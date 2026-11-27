package com.los.bank.customer.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Customer address details entity mapped to table c007.
 */
@Entity
@Table(name = "c007")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddress {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "residential_status_lookup_id")
    private Long residentialStatusLookupId;

    @Column(name = "residential_type_lookup_id")
    private Long residentialTypeLookupId;

    @Column(name = "flat_building_name", length = 150)
    private String flatBuildingName;

    @Column(name = "street_road", length = 150)
    private String streetRoad;

    @Column(name = "landmark", length = 150)
    private String landmark;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_lookup_id")
    private Long stateLookupId;

    @Column(name = "pincode", length = 6)
    private String pincode;
}
