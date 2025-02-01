package com.trading.repository;

import com.trading.entity.TradeEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<TradeEntity, UUID> {}
