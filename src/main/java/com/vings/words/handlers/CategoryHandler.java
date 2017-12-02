package com.vings.words.handlers;

import com.vings.words.model.Category;
import com.vings.words.repository.CategoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.BodyInserters.fromObject;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@Component
public class CategoryHandler {

    private static final String USER = "user";
    private static final String TITLE = "title";

    private CategoryRepository categoryRepository;

    public CategoryHandler(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Mono<ServerResponse> get(ServerRequest serverRequest) {
        String user = serverRequest.pathVariable(USER);
        Flux<Category> categories = categoryRepository.findByUser(user);
        return categories.collectList().flatMap(data -> {
            if (data.isEmpty()) {
                return notFound().build();
            } else {
                return ok().body(fromObject(data));
            }
        });
    }

    public Mono<ServerResponse> create(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(Category.class)
                .filter(category -> category.getUser() != null && category.getTitle() != null)
                .flatMap(category -> ok().body(categoryRepository.save(category), Category.class))
                .switchIfEmpty(badRequest().body(Mono.just("Parameters isn't specified correctly"), String.class));

    }

    public Mono<ServerResponse> delete(ServerRequest serverRequest) {
        String user = serverRequest.pathVariable(USER);
        String name = serverRequest.pathVariable(TITLE);
        return categoryRepository.findByUserAndTitle(user, name).log()
                .flatMap(category -> categoryRepository.deleteByUserAndTitle(user, name)
                        .then(ok().build()))
                .switchIfEmpty(notFound().build());
    }
}