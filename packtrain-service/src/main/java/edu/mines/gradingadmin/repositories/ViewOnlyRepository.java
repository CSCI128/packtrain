package edu.mines.gradingadmin.repositories;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface ViewOnlyRepository<T, R> extends Repository<T, R> {
    long count();

    boolean existsById(R id);

    List<T> findAll();

    List<T> findAllById(Iterable<R> ids);

    Optional<T> findById(R id);
}
