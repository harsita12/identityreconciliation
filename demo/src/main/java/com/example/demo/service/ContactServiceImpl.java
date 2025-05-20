package com.example.demo.service;

import com.example.demo.dto.ContactResponse;
import com.example.demo.dto.IdentifyRequest;
import com.example.demo.dto.IdentifyResponse;
import com.example.demo.entity.Contact;
import com.example.demo.entity.LinkPrecedence;
import com.example.demo.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository repo;

    @Override
    @Transactional
    public IdentifyResponse identify(IdentifyRequest req) {
        // 1) find any existing contacts by email and/or phone
        List<Contact> byEmail = req.getEmail() != null
                ? repo.findByEmail(req.getEmail())
                : Collections.emptyList();
        List<Contact> byPhone = req.getPhoneNumber() != null
                ? repo.findByPhoneNumber(req.getPhoneNumber())
                : Collections.emptyList();

        // union into a Set
        Set<Contact> matched = new HashSet<>();
        matched.addAll(byEmail);
        matched.addAll(byPhone);

        Contact primary;
        if (matched.isEmpty()) {
            // no hits → new primary
            primary = Contact.builder()
                    .email(req.getEmail())
                    .phoneNumber(req.getPhoneNumber())
                    .linkPrecedence(LinkPrecedence.PRIMARY)
                    .build();
            repo.save(primary);
        }
        else {
            // 2) expand to full cluster (all siblings of any matched)
            Set<Contact> cluster = new HashSet<>(matched);
            for (Contact c : matched) {
                if (c.getLinkPrecedence() == LinkPrecedence.PRIMARY) {
                    cluster.addAll(repo.findByLinkedId(c.getId()));
                } else {
                    // secondary → pull in its parent + that parent’s secondaries
                    repo.findById(c.getLinkedId()).ifPresent(cluster::add);
                    cluster.addAll(repo.findByLinkedId(c.getLinkedId()));
                }
            }

            // 3) pick the oldest‐created record as the single primary
            primary = cluster.stream()
                    .min(Comparator.comparing(Contact::getCreatedAt))
                    .orElseThrow();

            // 4) merge any other primaries into secondaries of our chosen primary
            cluster.stream()
                    .filter(c -> c.getLinkPrecedence() == LinkPrecedence.PRIMARY)
                    .filter(c -> !c.getId().equals(primary.getId()))
                    .forEach(c -> {
                        c.setLinkPrecedence(LinkPrecedence.SECONDARY);
                        c.setLinkedId(primary.getId());
                        repo.save(c);
                    });

            // 5) finally, if this request brought new contact info
            boolean hasNewEmail = req.getEmail() != null
                    && cluster.stream().noneMatch(c -> req.getEmail().equals(c.getEmail()));
            boolean hasNewPhone = req.getPhoneNumber() != null
                    && cluster.stream().noneMatch(c -> req.getPhoneNumber().equals(c.getPhoneNumber()));

            if (hasNewEmail || hasNewPhone) {
                Contact sec = Contact.builder()
                        .email(req.getEmail())
                        .phoneNumber(req.getPhoneNumber())
                        .linkPrecedence(LinkPrecedence.SECONDARY)
                        .linkedId(primary.getId())
                        .build();
                repo.save(sec);
            }
        }

        // 6) build the response from primary + all its secondaries
        List<Contact> allRecs = new ArrayList<>();
        allRecs.add(primary);
        allRecs.addAll(repo.findByLinkedId(primary.getId()));

        ContactResponse cr = ContactResponse.builder()
                .primaryContactId(primary.getId())
                .emails(allRecs.stream()
                        .map(Contact::getEmail)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList()))
                .phoneNumbers(allRecs.stream()
                        .map(Contact::getPhoneNumber)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList()))
                .secondaryContactIds(allRecs.stream()
                        .filter(c -> c.getLinkPrecedence() == LinkPrecedence.SECONDARY)
                        .map(Contact::getId)
                        .collect(Collectors.toList()))
                .build();

        return IdentifyResponse.builder()
                .contact(cr)
                .build();
    }
}
