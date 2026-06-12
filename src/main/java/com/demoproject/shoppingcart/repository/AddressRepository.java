package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.Address;
import com.demoproject.shoppingcart.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUser(AppUser user);
    Optional<Address> findByIdAndUser(Long id, AppUser user);
    Optional<Address> findByUserAndIsDefault(AppUser user, Boolean isDefault);
}
