package com.vings.words.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vings.words.model.Word;
import com.vings.words.repository.WordsRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashSet;

import static org.springframework.web.reactive.function.BodyInserters.fromObject;
import static org.springframework.web.reactive.function.server.ServerResponse.*;

@Component
public class DictionaryHandler {
    private static final String USER = "user";
    private static final String CATEGORY = "category";
    private static final String WORD = "word";
    private static final String LEARNED = "learned";
    private static final String TRANSLATION = "translation";
    private static final int CORRECT_ANSWERS = 99;

    private WordsRepository wordsRepository;

    public DictionaryHandler(WordsRepository wordsRepository) {
        this.wordsRepository = wordsRepository;
    }

    public Mono<ServerResponse> getWords(ServerRequest serverRequest) {
        String user = serverRequest.pathVariable(USER);
        String category = serverRequest.pathVariable(CATEGORY);
        Flux<Word> words = wordsRepository.findByUserAndCategory(user, category);
        return words.collectList().flatMap(data -> {
            if (data.isEmpty()) {
                return notFound().build();
            } else {
                return ok().body(fromObject(data));
            }
        });
    }

    public Mono<ServerResponse> getWordsByLearnedFilter(ServerRequest serverRequest) {
        String user = serverRequest.pathVariable(USER);
        String category = serverRequest.pathVariable(CATEGORY);
        boolean learned = Boolean.valueOf(serverRequest.pathVariable(LEARNED));
        Flux<Word> words = wordsRepository.findByUserAndCategory(user, category).filter(word -> learned ? word.getAnswers() > CORRECT_ANSWERS : word.getAnswers() < CORRECT_ANSWERS);
        return words.collectList().flatMap(data -> {
            if (data.isEmpty()) {
                return notFound().build();
            } else {
                return ok().body(fromObject(data));
            }
        });
    }

    public Mono<ServerResponse> save(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(Word.class)
                .filter(word -> word.getWord() != null && word.getTranslation() != null)
                .flatMap(word -> ok().body(wordsRepository.save(word), Word.class))
                .switchIfEmpty(badRequest().body(Mono.just("Parameters isn't specified correctly"), String.class));
    }

    public Mono<ServerResponse> deleteCategory(ServerRequest serverRequest) {
        String user = serverRequest.pathVariable(USER);
        String category = serverRequest.pathVariable(CATEGORY);
        wordsRepository.deleteByUserAndCategory(user, category);
        return wordsRepository.findOneByUserAndCategory(user, category)
                .flatMap(existingWords ->
                        wordsRepository.deleteByUserAndCategory(user, category)
                                .then(ok().build()))
                .switchIfEmpty(notFound().build());
    }

    public Mono<ServerResponse> updateWord(ServerRequest serverRequest) {
        String user = serverRequest.pathVariable(USER);
        String category = serverRequest.pathVariable(CATEGORY);
        String forWord = serverRequest.pathVariable(WORD);
        return serverRequest.bodyToMono(String.class).map(data -> {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(data);
                JsonNode translation = jsonNode.get(TRANSLATION);
                if(translation == null) {
                    throw new IllegalArgumentException("translation isn't specified");
                }
                return translation.textValue();
            } catch (Exception e) {
                throw Exceptions.propagate(e);
            }
        }).flatMap(translation -> ok().body(wordsRepository.updateTranslation(user, category, forWord, new HashSet<>(Arrays.asList(translation))), Word.class));
    }

    public Mono<ServerResponse> updateCategory(ServerRequest serverRequest) {
        return Mono.empty();
    }

    public Mono<ServerResponse> deleteWord(ServerRequest serverRequest) {
        String user = serverRequest.pathVariable(USER);
        String category = serverRequest.pathVariable(CATEGORY);
        String word = serverRequest.pathVariable(WORD);

        return wordsRepository.findByUserAndCategoryAndWord(user, category, word)
                .flatMap(existingWord ->
                        wordsRepository.deleteByUserAndCategoryAndWord(existingWord.getUser(), existingWord.getCategory(), existingWord.getWord())
                                .then(ok().build()))
                .switchIfEmpty(notFound().build());
    }

    public Mono<ServerResponse> deleteTranslation(ServerRequest serverRequest) {
        return Mono.empty();
    }
}
