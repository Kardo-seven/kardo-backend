package ru.kardo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.kardo.dto.request.RequestPreviewDtoResponse;
import ru.kardo.dto.request.UserRequestDtoRequest;
import ru.kardo.dto.request.UserRequestDtoResponse;
import ru.kardo.exception.ConflictException;
import ru.kardo.exception.NotFoundValidationException;
import ru.kardo.mapper.RequestPreviewMapper;
import ru.kardo.mapper.UserRequestMapper;
import ru.kardo.model.*;
import ru.kardo.model.enums.RequestStatus;
import ru.kardo.repo.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRequestServiceImpl implements UserRequestService {

    private final ProfileRepo profileRepo;
    private final EventRepo eventRepo;
    private final UserRequestRepo userRequestRepo;
    private final UserRequestMapper userRequestMapper;
    private final RequestPreviewRepo requestPreviewRepo;
    private final RequestPreviewMapper requestPreviewMapper;
    private final GrandFinalEventRepo grandFinalEventRepo;

    @Override
    @Transactional
    public UserRequestDtoResponse postUserRequest(Long userId, Long eventId, UserRequestDtoRequest userRequestDtoRequest) {
        postUserRequestValidation(userRequestDtoRequest);
        Profile profile = profileRepo.findById(userId).orElseThrow(() ->
                new NotFoundValidationException("Profile with id: " + userId + " not found"));
        Event event = eventRepo.findById(eventId).orElseThrow(() ->
                new NotFoundValidationException("Event with id: " + eventId + " not found"));
        if (userRequestRepo.findUserRequestByProfileIdAndEventId(profile.getId(), event.getId()).isEmpty()) {
            UserRequest userRequest = UserRequest.builder()
                    .profile(profile)
                    .name(userRequestDtoRequest.getName())
                    .lastName(userRequestDtoRequest.getLastName())
                    .surName(userRequestDtoRequest.getSurName())
                    .phone(userRequestDtoRequest.getPhone())
                    .email(userRequestDtoRequest.getEmail())
                    .address(userRequestDtoRequest.getAddress())
                    .birthday(userRequestDtoRequest.getBirthday())
                    .typeOfSelection(userRequestDtoRequest.getTypeOfSelection())
                    .gender(userRequestDtoRequest.getGender())
                    .linkSet(new HashSet<>())
                    .event(event)
                    .directionSet(new HashSet<>())
                    .requestStatus(RequestStatus.SEND)
                    .build();
            userRequestDtoRequest.getDirectionEnumList().forEach(directionEnum -> userRequest.getDirectionSet().add(new Direction(directionEnum)));
            userRequestDtoRequest.getLinkList().forEach(link -> userRequest.getLinkSet().add(new Link(link)));
            userRequestRepo.save(userRequest);
            return userRequestMapper.toUserRequestDtoResponse(userRequest);
        } else {
            throw new ConflictException("User already registered on event with id: " + eventId);
        }
    }

    @Override
    @Transactional
    public UserRequestDtoResponse postUserRequestToGrandFinalEvent(Long userId, Long eventId, UserRequestDtoRequest userRequestDtoRequest) {
        postUserRequestValidation(userRequestDtoRequest);
        Profile profile = profileRepo.findById(userId).orElseThrow(() ->
                new NotFoundValidationException("Profile with id: " + userId + " not found"));
        GrandFinalEvent grandFinalEvent = grandFinalEventRepo.findById(eventId).orElseThrow(() ->
                new NotFoundValidationException("Event with id: " + eventId + " not found"));
        if (userRequestRepo.findUserRequestByProfileIdAndGrandFinalEventId(profile.getId(), grandFinalEvent.getId()).isEmpty()) {
            UserRequest userRequest = UserRequest.builder()
                    .profile(profile)
                    .name(userRequestDtoRequest.getName())
                    .lastName(userRequestDtoRequest.getLastName())
                    .surName(userRequestDtoRequest.getSurName())
                    .phone(userRequestDtoRequest.getPhone())
                    .email(userRequestDtoRequest.getEmail())
                    .address(userRequestDtoRequest.getAddress())
                    .birthday(userRequestDtoRequest.getBirthday())
                    .typeOfSelection(userRequestDtoRequest.getTypeOfSelection())
                    .gender(userRequestDtoRequest.getGender())
                    .linkSet(new HashSet<>())
                    .grandFinalEvent(grandFinalEvent)
                    .directionSet(new HashSet<>())
                    .requestStatus(RequestStatus.SEND)
                    .build();
            userRequestDtoRequest.getDirectionEnumList().forEach(directionEnum -> userRequest.getDirectionSet().add(new Direction(directionEnum)));
            userRequestDtoRequest.getLinkList().forEach(link -> userRequest.getLinkSet().add(new Link(link)));
            userRequestRepo.save(userRequest);
            return userRequestMapper.toUserRequestDtoResponse(userRequest);
        } else {
            throw new ConflictException("User already registered on event with id: " + eventId);
        }
    }

    @Override
    @Transactional
    public RequestPreviewDtoResponse uploadRequestPreview(Long userId, Long eventId, MultipartFile multipartFile) throws IOException {
        Profile profile = profileRepo.findById(userId).orElseThrow(() ->
                new NotFoundValidationException("Profile with id: " + userId + " not found"));
        Event event = eventRepo.findById(eventId).orElseThrow(() ->
                new NotFoundValidationException("Event with id: " + eventId + " not found"));
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss-"));
        String fileName = multipartFile.getOriginalFilename();
        String folderPath = "resources" + "/" + profile.getUser().getEmail() + "/requestPreview";
        Path path = Path.of(folderPath);
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
        Path target = Paths.get(folderPath + "/" + date + fileName);
        if (userRequestRepo.findUserRequestByProfileIdAndEventId(profile.getId(), event.getId()).isEmpty()) {
            UserRequest userRequest = UserRequest.builder()
                    .profile(profile)
                    .event(event)
                    .requestStatus(RequestStatus.DRAFT)
                    .build();
            userRequestRepo.save(userRequest);
            RequestPreview requestPreview = RequestPreview.builder()
                    .userRequest(userRequest)
                    .type(multipartFile.getContentType())
                    .title(date + fileName)
                    .link(folderPath + "/" + date + fileName)
                    .build();
            requestPreviewRepo.save(requestPreview);
            Files.copy(multipartFile.getInputStream(), target);
            return requestPreviewMapper.toRequestPreviewDtoResponse(requestPreview);
        } else {
            UserRequest userRequest = userRequestRepo.findUserRequestByProfileIdAndEventId(profile.getId(), event.getId()).get();
            if (requestPreviewRepo.findByUserRequestId(userRequest.getId()).isEmpty()) {
                RequestPreview requestPreview = RequestPreview.builder()
                        .userRequest(userRequest)
                        .type(multipartFile.getContentType())
                        .title(date + fileName)
                        .link(folderPath + "/" + date + fileName)
                        .build();
                requestPreviewRepo.save(requestPreview);
                Files.copy(multipartFile.getInputStream(), target);
                return requestPreviewMapper.toRequestPreviewDtoResponse(requestPreview);
            } else {
                throw new ConflictException("User request already registered");
            }
        }
    }

    @Override
    @Transactional
    public UserRequestDtoResponse patchUserRequest(Long userId, Long requestId, UserRequestDtoRequest userRequestDtoRequest) {
        UserRequest userRequest = userRequestRepo.findByProfileIdAndId(userId, requestId).orElseThrow(() ->
                new NotFoundValidationException("User request with id: " + requestId + " not found"));
        userRequestMapper.updateLinkList(userRequest, userRequestDtoRequest);
        userRequestMapper.updateDirectionList(userRequest, userRequestDtoRequest);
        userRequestMapper.updateUserRequestDtoResponse(userRequestDtoRequest, userRequest);
        userRequest.setRequestStatus(RequestStatus.SEND);
        userRequestRepo.save(userRequest);
        return userRequestMapper.toUserRequestDtoResponse(userRequest);
    }

    @Override
    public List<UserRequestDtoResponse> getUserRequests(Long userId) {
        List<UserRequest> userRequests = userRequestRepo.findAllByProfileId(userId);
        return userRequestMapper.toUserRequestDtoResponseList(userRequests);
    }

    private void postUserRequestValidation(UserRequestDtoRequest userRequestDtoRequest) {
        if (userRequestDtoRequest.getLinkList() == null) {
            userRequestDtoRequest.setLinkList(new ArrayList<>());
        }
        if (userRequestDtoRequest.getDirectionEnumList() == null) {
            userRequestDtoRequest.setDirectionEnumList(new ArrayList<>());
        }
        if (userRequestDtoRequest.getDirectionEnumList().size() > 2) {
            throw  new ConflictException("Request cannot have more than two directions");
        }
    }
}
