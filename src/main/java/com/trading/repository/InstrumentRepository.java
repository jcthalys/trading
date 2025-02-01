package com.trading.repository;

import com.trading.entity.InstrumentEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentRepository extends JpaRepository<InstrumentEntity, UUID> {
  Optional<InstrumentEntity> findBySymbol(String symbol);
}
