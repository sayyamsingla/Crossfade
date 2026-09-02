package com.crossfade.crossfade_backend.service;

import com.crossfade.crossfade_backend.model.UserTopArtist;
import com.crossfade.crossfade_backend.model.UserTopTrack;
import com.crossfade.crossfade_backend.repo.UserRepository;
import com.crossfade.crossfade_backend.repo.UserTopArtistRepository;
import com.crossfade.crossfade_backend.repo.UserTopTrackRepository;
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

    /**
     * Only the top N ranked items count toward the score (and the shared-item
     * lists), so a large synced history (top 50) doesn't dilute the union with
     * rarely-listened tail entries.
     */
    private static final int MAX_COMPARE_ITEMS = 20;

    @Autowired
    UserRepository userRepo;

    @Autowired
    UserTopArtistRepository userTopArtistRepo;

    @Autowired
    UserTopTrackRepository userTopTrackRepo;

    public CompatibilityResponse getCompatibility(Long userId, Long withUserId) {
        userRepo.findById(userId).orElseThrow(() -> new EntityNotFoundException("User Not Found " + userId));
        userRepo.findById(withUserId).orElseThrow(() -> new EntityNotFoundException("User Not Found " + withUserId));

        List<UserTopArtist> mine = userTopArtistRepo.findByUser_IdOrderByRankAsc(userId).stream()
                .limit(MAX_COMPARE_ITEMS).toList();
        List<UserTopArtist> theirs = userTopArtistRepo.findByUser_IdOrderByRankAsc(withUserId).stream()
                .limit(MAX_COMPARE_ITEMS).toList();
        List<UserTopTrack> myTracks = userTopTrackRepo.findByUser_IdOrderByRankAsc(userId).stream()
                .limit(MAX_COMPARE_ITEMS).toList();
        List<UserTopTrack> theirTracks = userTopTrackRepo.findByUser_IdOrderByRankAsc(withUserId).stream()
                .limit(MAX_COMPARE_ITEMS).toList();

        Map<Long, Integer> myArtistRanks = mine.stream()
                .collect(toMap(a -> a.getArtist().getId(), UserTopArtist::getRank, (a, b) -> a));
        Map<Long, Integer> theirArtistRanks = theirs.stream()
                .collect(toMap(a -> a.getArtist().getId(), UserTopArtist::getRank, (a, b) -> a));
        Map<Long, String> artistNames = new HashMap<>();
        mine.forEach(a -> artistNames.put(a.getArtist().getId(), a.getArtist().getName()));
        theirs.forEach(a -> artistNames.putIfAbsent(a.getArtist().getId(), a.getArtist().getName()));

        OverlapResult artistOverlap = computeOverlap(myArtistRanks, theirArtistRanks, artistNames);

        Map<Long, Integer> myTrackRanks = myTracks.stream()
                .collect(toMap(t -> t.getTrack().getId(), UserTopTrack::getRank, (a, b) -> a));
        Map<Long, Integer> theirTrackRanks = theirTracks.stream()
                .collect(toMap(t -> t.getTrack().getId(), UserTopTrack::getRank, (a, b) -> a));
        Map<Long, String> trackTitles = new HashMap<>();
        myTracks.forEach(t -> trackTitles.put(t.getTrack().getId(), t.getTrack().getTitle()));
        theirTracks.forEach(t -> trackTitles.putIfAbsent(t.getTrack().getId(), t.getTrack().getTitle()));

        OverlapResult trackOverlap = computeOverlap(myTrackRanks, theirTrackRanks, trackTitles);

        int score = (int) Math.round((artistOverlap.similarity() * 0.6 + trackOverlap.similarity() * 0.4) * 100);
        boolean noData = myArtistRanks.isEmpty() && theirArtistRanks.isEmpty()
                && myTrackRanks.isEmpty() && theirTrackRanks.isEmpty();
        String caption = buildCaption(score, noData);

        return new CompatibilityResponse(score, caption, artistOverlap.shared(), trackOverlap.shared());
    }

    private record OverlapResult(double similarity, List<String> shared) {}

    /**
     * Rank-weighted overlap (Ruzicka similarity): each ranked item gets a
     * weight of (MAX_COMPARE_ITEMS - rank + 1), so a #1 pick counts far more
     * than a #20 pick. Similarity is sum(min(weights)) / sum(max(weights))
     * across the union, which reduces to plain Jaccard if every present item
     * had the same weight, but here rewards overlap more when it's ranked
     * highly on both sides.
     */
    private OverlapResult computeOverlap(Map<Long, Integer> myRanks, Map<Long, Integer> theirRanks, Map<Long, String> names) {
        Set<Long> union = new HashSet<>(myRanks.keySet());
        union.addAll(theirRanks.keySet());

        double sumMin = 0;
        double sumMax = 0;
        for (Long id : union) {
            double myWeight = weightForRank(myRanks.get(id));
            double theirWeight = weightForRank(theirRanks.get(id));
            sumMin += Math.min(myWeight, theirWeight);
            sumMax += Math.max(myWeight, theirWeight);
        }
        double similarity = sumMax == 0 ? 0.0 : sumMin / sumMax;

        List<String> shared = union.stream()
                .filter(id -> myRanks.containsKey(id) && theirRanks.containsKey(id))
                .sorted(Comparator.comparingInt(id -> Math.min(myRanks.get(id), theirRanks.get(id))))
                .map(names::get)
                .toList();

        return new OverlapResult(similarity, shared);
    }

    private double weightForRank(Integer rank) {
        return rank == null ? 0.0 : MAX_COMPARE_ITEMS - rank + 1;
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
