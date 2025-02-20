/*
 * Urheberrechtshinweis: Diese Software ist urheberrechtlich geschützt. Das Urheberrecht liegt bei
 * Research Industrial Systems Engineering (RISE) Forschungs-, Entwicklungs- und Großprojektberatung GmbH,
 * soweit nicht im Folgenden näher gekennzeichnet.
 */
package com.rise_world.gematik.accesskeeper.server.service;

import com.rise_world.gematik.accesskeeper.server.dto.EntityStatementDTO;
import com.rise_world.gematik.accesskeeper.server.dto.OpenidProviderDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Objects.isNull;

@Service
public class EntityStatementCache {

    private static final Logger LOG = LoggerFactory.getLogger(EntityStatementCache.class);

    private final Clock clock;
    private final ConcurrentHashMap<String, CacheEntry> cache;

    public EntityStatementCache(Clock clock) {
        this.clock = clock;
        cache = new ConcurrentHashMap<>();
    }

    /**
     * {@code get} returns all cached {@link EntityStatementDTO entity statements}
     *
     * @return all cached {@link EntityStatementDTO entity statements}
     */
    public Collection<EntityStatementDTO> get() {
        return get(all()).toList();
    }

    /**
     * {@code findFirstIssuer} returns the first {@link EntityStatementDTO entity statement} for the given issuer
     * or {@link Optional#empty()} if the issuer was not present in the cache
     *
     * @param idpIss issuer of the {@link EntityStatementDTO entity statement}
     * @return {@link Optional} containing the first {@link EntityStatementDTO entity statement} of the given issuer or {@link Optional#empty()}
     */
    public Optional<EntityStatementDTO> findFirstIssuer(String idpIss) {
        return get(byIssuer(idpIss))
            .findFirst();
    }

    private Stream<EntityStatementDTO> get(Predicate<EntityStatementDTO> filter) {
        long now = clock.instant().getEpochSecond();
        var entityStatements = Stream.<EntityStatementDTO>builder();

        var iterator = cache.values().iterator();
        while (iterator.hasNext()) {

            var entry = iterator.next();
            if (entry.isExpired(now)) {
                LOG.info("Cache entry for openid provider {} expired, remove from entity statement cache", entry.statement().getIssuer());
                iterator.remove();
                continue;
            }

            if (filter.test(entry.statement())) {
                entityStatements.add(entry.statement());
            }
        }

        return entityStatements.build();
    }

    /**
     * {@code store} overwrites the cached value for the issuer of the given {@link EntityStatementDTO entity statement}
     *
     * @param entityStatement {@link EntityStatementDTO} to add/replace
     */
    public void store(EntityStatementDTO entityStatement) {
        addToCache(LoadTrigger.SCHEDULED).accept(entityStatement);
    }

    /**
     * {@code attemptUpdate} tries to refresh the cached value for the given {@code issuer} by using the {@code updater} {@link Function}.
     * Manually updating a value is only possible once. If an update is not possible, the {@code updater} will not be executed.
     * If an update was possible or not will not be reported to the caller.
     *
     * @param issuer  {@code issuer} to update
     * @param updater {@link Function} to load the {@link EntityStatementDTO entity statement} for the given issuer
     */
    public void attemptUpdate(String issuer, Function<OpenidProviderDTO, Optional<EntityStatementDTO>> updater) {
        var cachedEntityStatement = cache.get(issuer);
        if (isNull(cachedEntityStatement)) {
            LOG.info("Issuer {} not present in entity statement cache - skipping update", issuer);
            return;
        }

        if (!cachedEntityStatement.canReload()) {
            LOG.info("Entity statement for issuer {} already reloaded - skipping reload", issuer);
            return;
        }

        LOG.info("Trigger reload entity statement for issuer {}", issuer);
        updater.apply(cachedEntityStatement.statement()).ifPresent(addToCache(LoadTrigger.MANUAL));
    }

    private Consumer<EntityStatementDTO> addToCache(LoadTrigger trigger) {
        return entityStatement -> {
            LOG.info("Add openid provider {} with expiry {} to entity statement cache", entityStatement.getIssuer(), entityStatement.getExp());
            cache.put(entityStatement.getIssuer(), new CacheEntry(entityStatement, trigger));
        };
    }

    private static Predicate<EntityStatementDTO> all() {
        return entityStatement -> true;
    }

    private static Predicate<EntityStatementDTO> byIssuer(String issuer) {
        return entityStatement -> Objects.equals(entityStatement.getIssuer(), issuer);
    }

    private record CacheEntry(EntityStatementDTO statement, LoadTrigger trigger) {

        boolean isExpired(long now) {
            return statement().getExp() <= now;
        }

        boolean canReload() {
            return trigger == LoadTrigger.SCHEDULED;
        }
    }

    private enum LoadTrigger {
        SCHEDULED,
        MANUAL
    }
}
