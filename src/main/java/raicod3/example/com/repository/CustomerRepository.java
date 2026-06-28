package raicod3.example.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import raicod3.example.com.model.CustomerProfile;
import raicod3.example.com.model.User;

import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerProfile, UUID> {
    UUID user(User user);
}
