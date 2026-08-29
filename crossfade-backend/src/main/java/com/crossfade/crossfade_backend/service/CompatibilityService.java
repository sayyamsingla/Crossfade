package com.crossfade.crossfade_backend.service;

import com.crossfade.crossfade_backend.model.UserTopArtist;
import com.crossfade.crossfade_backend.model.UserTopGenre;
import com.crossfade.crossfade_backend.repo.UserRepository;
import com.crossfade.crossfade_backend.repo.UserTopArtistRepository;
import com.crossfade.crossfade_backend.repo.UserTopGenreRepository;
import com.crossfade.crossfade_backend.response.CompatibilityResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toMap;

@Service
public class CompatibilityService {

    @Autowired
    UserRepository userRepo;

    @Autowired
    UserTopArtistRepository userTopArtistRepo;

    @Autowired
    UserTopGenreRepository userTopGenreRepo;

    public CompatibilityResponse getCompatibility(Long userId, Long withUserId) {
        userRepo.findById(userId).orElseThrow(() -> new EntityNotFoundException("User Not Found " + userId));
        userRepo.findById(withUserId).orElseThrow(() -> new EntityNotFoundException("User Not Found " + withUserId));

        List<UserTopArtist> mine = userTopArtistRepo.findByUser_IdOrderByRankAsc(userId);
        List<UserTopArtist> theirs = userTopArtistRepo.findByUser_IdOrderByRankAsc(withUserId);
        List<UserTopGenre> myGenres = userTopGenreRepo.findByUser_IdOrderByPercentageDesc(userId);
        List<UserTopGenre> theirGenres = userTopGenreRepo.findByUser_IdOrderByPercentageDesc(withUserId);

        Map<Long, Integer> myArtistRanks = mine.stream()
                .collect(toMap(a -> a.getArtist().getId(), UserTopArtist::getRank, (a, b) -> a));
        Map<Long, Integer> theirArtistRanks = theirs.stream()
                .collect(toMap(a -> a.getArtist().getId(), UserTopArtist::getRank, (a, b) -> a));
        Map<Long, String> artistNames = new HashMap<>();
        mine.forEach(a -> artistNames.put(a.getArtist().getId(), a.getArtist().getName()));
        theirs.forEach(a -> artistNames.putIfAbsent(a.getArtist().getId(), a.getArtist().getName()));

        Set<Long> artistUnion = new HashSet<>(myArtistRanks.keySet());
        artistUnion.addAll(theirArtistRanks.keySet());
        Set<Long> sharedArtistIds = new HashSet<>(myArtistRanks.keySet());
        sharedArtistIds.retainAll(theirArtistRanks.keySet());
        double artistJaccard = artistUnion.isEmpty() ? 0.0 : (double) sharedArtistIds.size() / artistUnion.size();

        List<String> sharedArtists = sharedArtistIds.stream()
                .sorted(Comparator.comparingInt(id -> Math.min(myArtistRanks.get(id), theirArtistRanks.get(id))))
                .map(artistNames::get)
                .toList();

        Map<String, Integer> myGenreByKey = myGenres.stream()
                .collect(toMap(g -> g.getGenreName().toLowerCase(), UserTopGenre::getPercentage, (a, b) -> a));
        Map<String, Integer> theirGenreByKey = theirGenres.stream()
                .collect(toMap(g -> g.getGenreName().toLowerCase(), UserTopGenre::getPercentage, (a, b) -> a));
        Map<String, String> genreDisplayNames = new HashMap<>();
        myGenres.forEach(g -> genreDisplayNames.put(g.getGenreName().toLowerCase(), g.getGenreName()));
        theirGenres.forEach(g -> genreDisplayNames.putIfAbsent(g.getGenreName().toLowerCase(), g.getGenreName()));

        Set<String> genreUnion = new HashSet<>(myGenreByKey.keySet());
        genreUnion.addAll(theirGenreByKey.keySet());
        Set<String> sharedGenreKeys = new HashSet<>(myGenreByKey.keySet());
        sharedGenreKeys.retainAll(theirGenreByKey.keySet());
        double genreJaccard = genreUnion.isEmpty() ? 0.0 : (double) sharedGenreKeys.size() / genreUnion.size();

        List<String> sharedGenres = sharedGenreKeys.stream()
                .sorted(Comparator.<String>comparingInt(k -> myGenreByKey.get(k) + theirGenreByKey.get(k)).reversed())
                .map(genreDisplayNames::get)
                .toList();

        int score = (int) Math.round((artistJaccard * 0.5 + genreJaccard * 0.5) * 100);
        boolean noData = myArtistRanks.isEmpty() && theirArtistRanks.isEmpty()
                && myGenreByKey.isEmpty() && theirGenreByKey.isEmpty();
        String caption = buildCaption(score, noData);

        return new CompatibilityResponse(score, caption, sharedArtists, sharedGenres);
    }

    private String buildCaption(int score, boolean noData) {
        if (noData) return "Not enough listening data yet to compare taste.";
        if (score >= 80) return "You two are basically the same playlist.";
        if (score >= 60) return "Strong overlap, you'd probably vibe at the same show.";
        if (score >= 35) return "Some common ground, plenty of room to swap recs.";
        if (score >= 15) return "A little overlap, mostly different lanes.";
        return "Pretty different taste, could be fun to explore each other's world.";
    }
}
