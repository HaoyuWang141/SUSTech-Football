package com.sustech.football.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sustech.football.entity.*;
import com.sustech.football.exception.BadRequestException;
import com.sustech.football.exception.ResourceNotFoundException;
import com.sustech.football.model.event.VoEvent;
import com.sustech.football.service.*;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@CrossOrigin
@RestController
@RequestMapping("/event")
@Tag(name = "Event Controller", description = "赛事管理接口")
public class EventController {
    @Autowired
    private EventService eventService;
    @Autowired
    private UserService userService;
    @Autowired
    private TeamService teamService;
    @Autowired
    private MatchService matchService;
    @Autowired
    private RefereeService refereeService;
    @Autowired
    private EventGroupService eventGroupService;
    @Autowired
    private EventGroupTeamService eventGroupTeamService;
    @Autowired
    private EventStageService eventStageService;
    @Autowired
    private EventStageTagService eventStageTagService;
    @Autowired
    private EventTeamService eventTeamService;

    public record Match_GET(
            Long matchId,
            Long homeTeamId,
            Long awayTeamId,
            Timestamp time,
            String status,
            String description,
            Integer homeTeamScore,
            Integer awayTeamScore,
            Integer homeTeamPenalty,
            Integer awayTeamPenalty
    ) {
    }

    @PostMapping("/create")
    @Operation(summary = "创建赛事", description = "创建一个新的赛事")
    public String createEvent(Long ownerId, @RequestBody Event event) {
        if (ownerId == null || event == null) {
            throw new BadRequestException("传参为空");
        }
        if (userService.getById(ownerId) == null) {
            throw new BadRequestException("赛事所有者不存在");
        }
        if (event.getEventId() != null) {
            throw new BadRequestException("赛事不能id");
        }
        if (!eventService.createEvent(event)) {
            throw new BadRequestException("创建赛事失败");
        }
        if (!eventService.inviteManager(new EventManager(event.getEventId(), ownerId, true))) {
            throw new BadRequestException("比赛管理员设置失败");
        }
        return "创建赛事成功";
    }

    @GetMapping("/get")
    @Operation(summary = "获取赛事", description = "根据 ID 获取赛事详细信息")
    @Parameter(name = "id", description = "赛事 ID", required = true)
    public VoEvent getEvent(Long id) {
        if (id == null) {
            throw new BadRequestException("传入的赛事ID为空");
        }
        Event event = null;
        try {
            event = eventService.getDetailedEvent(id);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        if (event == null) {
            throw new BadRequestException("传入的赛事ID不存在");
        }
        VoEvent voEvent = new VoEvent();
        voEvent.setEventId(event.getEventId());
        voEvent.setName(event.getName());
        voEvent.setDescription(event.getDescription());
        voEvent.setManagerList(event.getManagerIdList()
                .stream()
                .map(userId -> {
                    User user = userService.getById(userId);
                    VoEvent.VoManager voManager = new VoEvent.VoManager();
                    voManager.setUserId(user.getUserId());
                    voManager.setNickName(user.getNickName());
                    voManager.setAvatarUrl(user.getAvatarUrl());
                    return voManager;
                })
                .toList());
        voEvent.setTeamList(event.getTeamList()
                .stream()
                .map(team -> {
                    VoEvent.VoTeamInfo voTeamInfo = new VoEvent.VoTeamInfo();
                    voTeamInfo.setId(team.getId());
                    voTeamInfo.setName(team.getName());
                    voTeamInfo.setLogoUrl(team.getLogo());
                    voTeamInfo.setPlayerCount(team.getPlayerCount());
                    return voTeamInfo;
                })
                .toList());
        voEvent.setGroupList(event.getGroupList()
                .stream()
                .map(group -> {
                    VoEvent.VoGroup voGroup = new VoEvent.VoGroup();
                    voGroup.setGroupId(group.getGroupId());
                    voGroup.setName(group.getName());
                    voGroup.setTeamList(group.getTeamList()
                            .stream()
                            .map(team -> {
                                VoEvent.VoGroup.VoTeam voTeam = new VoEvent.VoGroup.VoTeam();
                                VoEvent.VoTeamInfo voTeamInfo = new VoEvent.VoTeamInfo();
                                voTeamInfo.setId(team.getTeam().getId());
                                voTeamInfo.setName(team.getTeam().getName());
                                voTeamInfo.setLogoUrl(team.getTeam().getLogo());
                                voTeamInfo.setPlayerCount(team.getTeam().getPlayerCount());
                                voTeam.setTeam(voTeamInfo);
                                voTeam.setNumWins(team.getNumWins());
                                voTeam.setNumDraws(team.getNumDraws());
                                voTeam.setNumLosses(team.getNumLosses());
                                voTeam.setNumGoalsFor(team.getNumGoalsFor());
                                voTeam.setNumGoalsAgainst(team.getNumGoalsAgainst());
                                voTeam.setScore(team.getScore());
                                return voTeam;
                            })
                            .toList());
                    return voGroup;
                })
                .toList());
        voEvent.setMatchList(event.getMatchList()
                .stream()
                .map(match -> {
                    VoEvent.VoMatch voMatch = new VoEvent.VoMatch();
                    voMatch.setMatchId(match.getMatchId());
                    VoEvent.VoTeamInfo homeTeam = new VoEvent.VoTeamInfo();
                    homeTeam.setId(match.getHomeTeam().getTeamId());
                    homeTeam.setName(match.getHomeTeam().getName());
                    homeTeam.setLogoUrl(match.getHomeTeam().getLogoUrl());
                    VoEvent.VoTeamInfo awayTeam = new VoEvent.VoTeamInfo();
                    awayTeam.setId(match.getAwayTeam().getTeamId());
                    awayTeam.setName(match.getAwayTeam().getName());
                    awayTeam.setLogoUrl(match.getAwayTeam().getLogoUrl());
                    voMatch.setHomeTeam(homeTeam);
                    voMatch.setAwayTeam(awayTeam);
                    voMatch.setTime(match.getTime());
                    voMatch.setStatus(match.getStatus());
                    voMatch.setDescription(match.getDescription());
                    voMatch.setMatchLocation("暂无地点信息");
                    voMatch.setHomeTeamScore(match.getHomeTeamScore());
                    voMatch.setAwayTeamScore(match.getAwayTeamScore());
                    voMatch.setHomeTeamPenalty(match.getHomeTeamPenalty());
                    voMatch.setAwayTeamPenalty(match.getAwayTeamPenalty());
                    voMatch.setStage(match.getMatchEvent().getMatchStage());
                    voMatch.setTag(match.getMatchEvent().getMatchTag());
                    return voMatch;
                })
                .toList());
        voEvent.setStageList(event.getStageList()
                .stream()
                .map(stage -> {
                    VoEvent.Stage voStage = new VoEvent.Stage();
                    voStage.setStageName(stage.getStageName());
                    voStage.setTags(stage.getTags()
                            .stream()
                            .map(tag -> {
                                VoEvent.Tag voTag = new VoEvent.Tag();
                                voTag.setTagName(tag.getTagName());
                                voTag.setMatches(tag.getMatches()
                                        .stream()
                                        .map(match -> {
                                            VoEvent.VoMatch voMatch = new VoEvent.VoMatch();
                                            voMatch.setMatchId(match.getMatchId());
                                            VoEvent.VoTeamInfo homeTeam = new VoEvent.VoTeamInfo();
                                            homeTeam.setId(match.getHomeTeam().getTeamId());
                                            homeTeam.setName(match.getHomeTeam().getName());
                                            homeTeam.setLogoUrl(match.getHomeTeam().getLogoUrl());
                                            VoEvent.VoTeamInfo awayTeam = new VoEvent.VoTeamInfo();
                                            awayTeam.setId(match.getAwayTeam().getTeamId());
                                            awayTeam.setName(match.getAwayTeam().getName());
                                            awayTeam.setLogoUrl(match.getAwayTeam().getLogoUrl());
                                            voMatch.setHomeTeam(homeTeam);
                                            voMatch.setAwayTeam(awayTeam);
                                            voMatch.setTime(match.getTime());
                                            voMatch.setStatus(match.getStatus());
                                            voMatch.setDescription(match.getDescription());
                                            voMatch.setMatchLocation("暂无地点信息");
                                            voMatch.setHomeTeamScore(match.getHomeTeamScore());
                                            voMatch.setAwayTeamScore(match.getAwayTeamScore());
                                            voMatch.setHomeTeamPenalty(match.getHomeTeamPenalty());
                                            voMatch.setAwayTeamPenalty(match.getAwayTeamPenalty());
                                            voMatch.setStage(match.getMatchEvent().getMatchStage());
                                            voMatch.setTag(match.getMatchEvent().getMatchTag());
                                            return voMatch;
                                        })
                                        .toList());
                                return voTag;
                            })
                            .toList());
                    return voStage;
                })
                .toList());
        return voEvent;
    }

    @GetMapping("/getAll")
    @Operation(summary = "获取所有赛事", description = "获取所有赛事的详细信息")
    public List<Event> getAllEvents() {
        return eventService.list().stream().sorted(Comparator.comparing(Event::getEventId)).toList();
    }

    @GetMapping("/getByIdList")
    @Operation(summary = "获取赛事", description = "根据 ID 列表获取赛事详细信息")
    @Parameter(name = "idList", description = "赛事 ID 列表", required = true)
    public List<Event> getEventsByIdLists(@RequestParam List<Long> idList) {
        return eventService.listByIds(idList);
    }

    @PutMapping("/update")
    @Operation(summary = "更新赛事", description = "更新赛事信息")
    public void updateEvent(@RequestBody Event event) {
        if (event == null) {
            throw new BadRequestException("传入的赛事为空");
        }
        if (event.getEventId() == null) {
            throw new BadRequestException("传入的赛事ID为空");
        }
        if (!eventService.updateEvent(event)) {
            throw new BadRequestException("更新赛事失败");
        }
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除赛事", description = "删除赛事")
    @Parameters({
            @Parameter(name = "eventId", description = "赛事 ID", required = true),
            @Parameter(name = "userId", description = "删除者 ID", required = true)
    })
    public void deleteEvent(Long eventId, Long userId) {
        if (eventId == null || userId == null) {
            throw new BadRequestException("传入的赛事或删除者ID为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (userId != 0 && userService.getById(userId) == null) {
            throw new ResourceNotFoundException("删除者不存在");
        }
        // TODO：记录删除者ID至数据库表or日志中，现在懒得写
        if (!eventService.deleteEvent(eventId)) {
            throw new BadRequestException("删除赛事失败");
        }
    }

    @PostMapping("/manager/invite")
    @Operation(summary = "邀请管理员", description = "邀请管理员")
    @Parameters({
            @Parameter(name = "eventId", description = "赛事 ID", required = true),
            @Parameter(name = "managerId", description = "管理员 ID", required = true)
    })
    public void inviteManager(Long eventId, Long managerId) {
        if (eventId == null || managerId == null) {
            throw new BadRequestException("传入的赛事ID或管理员ID为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (userService.getById(managerId) == null) {
            throw new ResourceNotFoundException("管理员不存在");
        }
        if (!eventService.inviteManager(new EventManager(eventId, managerId, false))) {
            throw new RuntimeException("邀请管理员失败");
        }
    }

    @GetMapping("/manager/getAll")
    @Operation(summary = "获取赛事所有管理员", description = "提供赛事 ID，获取其所有管理员")
    @Parameter(name = "eventId", description = "赛事 ID", required = true)
    public List<Long> getManagers(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("传入的赛事ID为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        return eventService.getManagers(eventId);
    }

    @DeleteMapping("/manager/delete")
    @Operation(summary = "删除管理员", description = "删除管理员")
    @Parameters({
            @Parameter(name = "eventId", description = "赛事 ID", required = true),
            @Parameter(name = "managerId", description = "管理员 ID", required = true)
    })
    public void deleteManager(Long eventId, Long managerId) {
        if (eventId == null || managerId == null) {
            throw new BadRequestException("传入的赛事ID或管理员ID为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (userService.getById(managerId) == null) {
            throw new ResourceNotFoundException("管理员不存在");
        }
        if (!eventService.deleteManager(new EventManager(eventId, managerId, false))) {
            throw new RuntimeException("删除管理员失败");
        }
    }

    @PostMapping("/team/invite")
    @Operation(summary = "邀请球队", description = "邀请球队参与赛事")
    @Parameters({
            @Parameter(name = "eventId", description = "赛事 ID", required = true),
            @Parameter(name = "teamId", description = "球队 ID", required = true)
    })
    public void inviteTeam(Long eventId, Long teamId) {
        if (eventId == null || teamId == null) {
            throw new BadRequestException("传入的赛事ID或球队ID为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (teamService.getById(teamId) == null) {
            throw new ResourceNotFoundException("球队不存在");
        }
        if (!eventService.inviteTeam(new EventTeamRequest(
                eventId, teamId, EventTeamRequest.TYPE_INVITATION
        ))) {
            throw new RuntimeException("邀请球队失败");
        }
    }

    @GetMapping("/team/getInvitations")
    @Operation(summary = "获取球队邀请", description = "获取赛事的球队邀请")
    @Parameter(name = "eventId", description = "赛事 ID", required = true)
    public List<EventTeamRequest> getTeamInvitations(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("传入的赛事ID为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        return eventService.getTeamInvitations(eventId);
    }

    @GetMapping("/team/getApplications")
    @Operation(summary = "获取球队申请", description = "获取赛事的球队申请")
    @Parameter(name = "eventId", description = "赛事 ID", required = true)
    public List<EventTeamRequest> getTeamApplications(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("传入的赛事ID为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        return eventService.getTeamApplications(eventId);
    }

    @PostMapping("/team/replyApplication")
    @Operation(summary = "回复球队申请", description = "回复赛事的球队申请")
    @Parameters({
            @Parameter(name = "eventId", description = "赛事 ID", required = true),
            @Parameter(name = "teamId", description = "球队 ID", required = true),
            @Parameter(name = "accepted", description = "是否接受", required = true)
    })
    public void replyTeamApplication(Long eventId, Long teamId, Boolean accepted) {
        if (eventId == null || teamId == null || accepted == null) {
            throw new BadRequestException("传入的参数含有空值");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (teamService.getById(teamId) == null) {
            throw new ResourceNotFoundException("球队不存在");
        }
        if (!eventService.replyTeamApplication(new EventTeamRequest(
                eventId, teamId, EventTeamRequest.TYPE_APPLICATION,
                accepted ? EventTeamRequest.STATUS_ACCEPTED : EventTeamRequest.STATUS_REJECTED
        ))) {
            throw new RuntimeException("回复球队申请失败");
        }
    }

    @GetMapping("/team/getAll")
    @Operation(summary = "获取赛事所有球队", description = "提供赛事 ID，获取赛事的所有球队")
    @Parameter(name = "eventId", description = "赛事 ID", required = true)
    public List<Team> getTeams(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("传入的赛事ID为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        return eventService.getTeams(eventId);
    }

    @DeleteMapping("/team/delete")
    @Operation(summary = "删除球队", description = "删除赛事的球队")
    @Parameters({
            @Parameter(name = "eventId", description = "赛事 ID", required = true),
            @Parameter(name = "teamId", description = "球队 ID", required = true)
    })
    public void deleteTeam(Long eventId, Long teamId) {
        if (eventId == null || teamId == null) {
            throw new BadRequestException("传入的赛事ID或球队ID为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (teamService.getById(teamId) == null) {
            throw new ResourceNotFoundException("球队不存在");
        }
        if (!eventService.deleteTeam(new EventTeam(eventId, teamId))) {
            throw new RuntimeException("删除球队失败");
        }
    }

    @PostMapping("/group/new")
    @Deprecated
    @Operation(summary = "创建分组", description = "创建赛事的新分组")
    @Parameters({
            @Parameter(name = "eventId", description = "赛事 ID", required = true),
            @Parameter(name = "groupName", description = "组名", required = true)
    })
    public EventGroup newGroup(Long eventId, String groupName) {
        if (eventId == null || groupName == null) {
            throw new BadRequestException("传入的赛事ID或组名为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        EventGroup group = new EventGroup(eventId, groupName);
        if (!eventGroupService.save(group)) {
            throw new RuntimeException("创建分组失败");
        }
        return group;
    }

    @GetMapping("/group/getByEventId")
    @Operation(summary = "获取赛事所有分组", description = "提供赛事 ID，获取赛事的所有分组")
    @Parameter(name = "eventId", description = "赛事 ID", required = true)
    public List<EventGroup> getGroups(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("传入的赛事ID为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        QueryWrapper<EventGroup> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("event_id", eventId);
        return eventGroupService.list(queryWrapper);
    }

    @PutMapping("/group/updateName")
    @Operation(summary = "更新分组名", description = "更新分组名")
    @Parameters({
            @Parameter(name = "groupId", description = "分组 ID", required = true),
            @Parameter(name = "name", description = "新组名", required = true)
    })
    public void updateGroup(Long groupId, String name) {
        if (groupId == null || name == null) {
            throw new BadRequestException("传入的分组ID或组名为空");
        }
        if (eventGroupService.getById(groupId) == null) {
            throw new ResourceNotFoundException("分组不存在");
        }
        EventGroup group = eventGroupService.getById(groupId);
        group.setName(name);
        if (!eventGroupService.updateById(group)) {
            throw new RuntimeException("更新分组名失败");
        }
    }

    @DeleteMapping("/group/delete")
    @Operation(summary = "删除分组", description = "提供分组 ID，删除分组")
    @Parameter(name = "groupId", description = "分组 ID", required = true)
    public void deleteGroup(Long groupId) {
        if (groupId == null) {
            throw new BadRequestException("传入的分组ID为空");
        }
        if (eventGroupService.getById(groupId) == null) {
            throw new ResourceNotFoundException("分组不存在");
        }
        if (eventGroupTeamService.remove(new QueryWrapper<EventGroupTeam>().eq("group_id", groupId))) {
            throw new RuntimeException("删除分组内的球队失败");
        }
        if (!eventGroupService.removeById(groupId)) {
            throw new RuntimeException("删除分组失败");
        }
    }

    @PostMapping("/group/addTeam")
    @Operation(summary = "添加球队到分组", description = "提供分组和球队ID，添加球队到分组")
    @Parameters({
            @Parameter(name = "groupId", description = "分组 ID", required = true),
            @Parameter(name = "teamId", description = "球队 ID", required = true)
    })
    public void addTeamIntoGroup(Long groupId, Long teamId) {
        if (groupId == null || teamId == null) {
            throw new BadRequestException("传参为空");
        }
        if (eventGroupService.getById(groupId) == null) {
            throw new ResourceNotFoundException("分组不存在");
        }
        if (teamService.getById(teamId) == null) {
            throw new ResourceNotFoundException("球队不存在");
        }
        if (!eventGroupTeamService.saveOrUpdateByMultiId(new EventGroupTeam(groupId, teamId))) {
            throw new RuntimeException("添加球队到分组失败");
        }
    }

    @DeleteMapping("/group/deleteTeam")
    @Operation(summary = "删除分组内球队", description = "提供分组和球队ID，删除分组内球队")
    @Parameters({
            @Parameter(name = "groupId", description = "分组 ID", required = true),
            @Parameter(name = "teamId", description = "球队 ID", required = true)
    })
    public void deleteTeamFromGroup(Long groupId, Long teamId) {
        if (groupId == null || teamId == null) {
            throw new BadRequestException("传入的分组ID或球队ID为空");
        }
        if (eventGroupService.getById(groupId) == null) {
            throw new ResourceNotFoundException("分组不存在");
        }
        if (teamService.getById(teamId) == null) {
            throw new ResourceNotFoundException("球队不存在");
        }
        if (!eventGroupTeamService.deleteByMultiId(new EventGroupTeam(groupId, teamId))) {
            throw new RuntimeException("删除球队失败");
        }
    }

    @PostMapping("/stage/new")
    @Operation(summary = "创建阶段", description = "创建赛事的新阶段")
    @Parameters({
            @Parameter(name = "eventId", description = "赛事 ID", required = true),
            @Parameter(name = "stageName", description = "阶段名", required = true)
    })
    public EventStage newStage(Long eventId, String stageName) {
        if (eventId == null || stageName == null) {
            throw new BadRequestException("传入的赛事ID或阶段名为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        EventStage stage = new EventStage(eventId, stageName);
        if (!eventStageService.saveOrUpdateByMultiId(stage)) {
            throw new RuntimeException("创建阶段失败");
        }
        return stage;
    }

    @DeleteMapping("/stage/delete")
    @Operation(summary = "删除阶段", description = "删除赛事的阶段")
    @Parameters({
            @Parameter(name = "eventId", description = "赛事 ID", required = true),
            @Parameter(name = "stageName", description = "阶段名", required = true)
    })
    public void deleteStage(Long eventId, String stageName) {
        if (eventId == null || stageName == null) {
            throw new BadRequestException("传入的赛事ID或阶段名为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (eventStageService.selectByMultiId(new EventStage(eventId, stageName)) == null) {
            throw new ResourceNotFoundException("阶段不存在");
        }
        if (!eventStageService.deleteByMultiId(new EventStage(eventId, stageName))) {
            throw new RuntimeException("删除阶段失败");
        }
    }

    @PostMapping("/tag/new")
    @Operation(summary = "创建标签", description = "创建赛事的新标签")
    @Parameters({
            @Parameter(name = "eventId", description = "赛事 ID", required = true),
            @Parameter(name = "stageName", description = "阶段名", required = true),
            @Parameter(name = "tagName", description = "标签名", required = true)
    })
    public EventStageTag newTag(Long eventId, String stageName, String tagName) {
        if (eventId == null || stageName == null || tagName == null) {
            throw new BadRequestException("传入的赛事ID或阶段名或标签名为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (eventStageService.selectByMultiId(new EventStage(eventId, stageName)) == null) {
            throw new ResourceNotFoundException("阶段不存在");
        }
        EventStageTag tag = new EventStageTag(eventId, stageName, tagName);
        if (!eventStageTagService.saveOrUpdateByMultiId(tag)) {
            throw new RuntimeException("创建标签失败");
        }
        return tag;
    }

    @DeleteMapping("/tag/delete")
    @Operation(summary = "删除标签", description = "删除赛事的标签")
    @Parameters({
            @Parameter(name = "eventId", description = "赛事 ID", required = true),
            @Parameter(name = "stageName", description = "阶段名", required = true),
            @Parameter(name = "tagName", description = "标签名", required = true)
    })
    public void deleteTag(Long eventId, String stageName, String tagName) {
        if (eventId == null || stageName == null || tagName == null) {
            throw new BadRequestException("传入的赛事ID或阶段名或标签名为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (eventStageService.selectByMultiId(new EventStage(eventId, stageName)) == null) {
            throw new ResourceNotFoundException("阶段不存在");
        }
        if (eventStageTagService.selectByMultiId(new EventStageTag(eventId, stageName, tagName)) == null) {
            throw new ResourceNotFoundException("标签不存在");
        }
        if (!eventStageTagService.deleteByMultiId(new EventStageTag(eventId, stageName, tagName))) {
            throw new RuntimeException("删除标签失败");
        }
    }

    @PostMapping("/match/add")
    @Operation(summary = "添加比赛", description = "添加赛事的新比赛")
    @Parameters({
            @Parameter(name = "eventId", description = "赛事 ID", required = true),
            @Parameter(name = "stage", description = "阶段名", required = true),
            @Parameter(name = "tag", description = "标签名", required = true),
            @Parameter(name = "time", description = "比赛时间", required = true),
            @Parameter(name = "homeTeamId", description = "主队 ID", required = true),
            @Parameter(name = "awayTeamId", description = "客队 ID", required = true)
    })
    public void addMatch(Long eventId, String stage, String tag, Timestamp time, Long homeTeamId, Long awayTeamId) {
        if (eventId == null || stage == null || tag == null || time == null || homeTeamId == null || awayTeamId == null) {
            throw new BadRequestException("传参含空值");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (teamService.getById(homeTeamId) == null) {
            throw new ResourceNotFoundException("主队不存在");
        }
        if (teamService.getById(awayTeamId) == null) {
            throw new ResourceNotFoundException("客队不存在");
        }
        List<EventTeam> teams = eventTeamService.list(new QueryWrapper<EventTeam>().eq("event_id", eventId));
        if (teams.stream().noneMatch(t -> t.getTeamId().equals(homeTeamId))) {
            throw new BadRequestException("主队不在赛事中");
        }
        if (teams.stream().noneMatch(t -> t.getTeamId().equals(awayTeamId))) {
            throw new BadRequestException("客队不在赛事中");
        }
        Match match = new Match();
        match.setHomeTeamId(homeTeamId);
        match.setAwayTeamId(awayTeamId);
        match.setTime(time);

        if (!eventService.addMatch(eventId, match, stage, tag)) {
            throw new RuntimeException("新建失败");
        }
    }

    @GetMapping("/match/getAll")
    @Operation(summary = "获取赛事所有比赛", description = "提供赛事 ID，获取赛事的所有比赛")
    @Parameter(name = "eventId", description = "赛事 ID", required = true)
    public List<Match> getMatches(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("传入的赛事ID为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        return eventService.getMatches(eventId);
    }

    @PutMapping("/match/update")
    @Operation(summary = "更新比赛", description = "更新赛事的比赛")
    public void updateMatch(Long eventId, @RequestBody Match_GET match) {
        if (eventId == null || match == null) {
            throw new BadRequestException("传参含空值");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (match.matchId() == null) {
            throw new BadRequestException("比赛id为空");
        }
        Match match_update = matchService.getById(match.matchId());
        if (match_update == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        if (eventService.getMatches(eventId).stream().noneMatch(m -> m.getMatchId().equals(match.matchId()))) {
            throw new BadRequestException("赛事不含比赛");
        }
        // 也可以用Optional，这样允许部分更新
//        match_update.setHomeTeamId(Optional.ofNullable(match.homeTeamId()).orElse(match_update.getHomeTeamId()));
        match_update.setHomeTeamId(match.homeTeamId());
        match_update.setAwayTeamId(match.awayTeamId());
        match_update.setTime(match.time());
        match_update.setStatus(match.status());
        match_update.setDescription(match.description());
        match_update.setHomeTeamScore(match.homeTeamScore());
        match_update.setAwayTeamScore(match.awayTeamScore());
        match_update.setHomeTeamPenalty(match.homeTeamPenalty());
        match_update.setAwayTeamPenalty(match.awayTeamPenalty());
        if (!matchService.updateById(match_update)) {
            throw new RuntimeException("更新比赛失败");
        }
    }

    @DeleteMapping("/match/delete")
    @Operation(summary = "删除比赛", description = "删除赛事的比赛")
    @Parameters({
            @Parameter(name = "eventId", description = "赛事 ID", required = true),
            @Parameter(name = "matchId", description = "比赛 ID", required = true)
    })
    public void deleteMatch(Long eventId, Long matchId) {
        if (eventId == null || matchId == null) {
            throw new BadRequestException("传参含空值");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (matchService.getById(matchId) == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        if (!eventService.deleteMatch(new EventMatch(eventId, matchId))) {
            throw new RuntimeException("删除比赛失败");
        }
    }

    @PostMapping("/match/setReferee")
    @Operation(summary = "设置裁判", description = "直接设置比赛的裁判")
    @Parameters({
            @Parameter(name = "eventId", description = "赛事 ID", required = true),
            @Parameter(name = "matchId", description = "比赛 ID", required = true),
            @Parameter(name = "refereeId", description = "裁判 ID", required = true)
    })
    public void setMatchReferee(Long eventId, Long matchId, Long refereeId) {
        if (eventId == null || matchId == null || refereeId == null) {
            throw new BadRequestException("传参含空值");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (matchService.getById(matchId) == null) {
            throw new ResourceNotFoundException("比赛不存在");
        }
        if (refereeService.getById(refereeId) == null) {
            throw new ResourceNotFoundException("裁判不存在");
        }
        if (!eventService.setMatchReferee(eventId, matchId, refereeId)) {
            throw new RuntimeException("设置裁判失败");
        }
    }

    @PostMapping("/referee/invite")
    @Operation(summary = "邀请裁判", description = "邀请裁判参与赛事")
    @Parameters({
            @Parameter(name = "eventId", description = "赛事 ID", required = true),
            @Parameter(name = "refereeId", description = "裁判 ID", required = true)
    })
    public void inviteReferee(Long eventId, Long refereeId) {
        if (eventId == null || refereeId == null) {
            throw new BadRequestException("传入的赛事ID或裁判ID为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (refereeService.getById(refereeId) == null) {
            throw new ResourceNotFoundException("裁判不存在");
        }
        if (!eventService.inviteReferee(new EventRefereeRequest(eventId, refereeId))) {
            throw new RuntimeException("邀请裁判失败");
        }
    }

    @GetMapping("/referee/getAll")
    @Operation(summary = "获取赛事所有裁判", description = "提供赛事 ID，获取赛事的所有裁判")
    @Parameter(name = "eventId", description = "赛事 ID", required = true)
    public List<Referee> getReferees(Long eventId) {
        if (eventId == null) {
            throw new BadRequestException("传入的赛事ID为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        return eventService.getReferees(eventId);
    }

    @DeleteMapping("/referee/delete")
    @Operation(summary = "删除裁判", description = "删除赛事的裁判")
    @Parameters({
            @Parameter(name = "eventId", description = "赛事 ID", required = true),
            @Parameter(name = "refereeId", description = "裁判 ID", required = true)
    })
    public void deleteReferee(Long eventId, Long refereeId) {
        if (eventId == null || refereeId == null) {
            throw new BadRequestException("传入的赛事ID或裁判ID为空");
        }
        if (eventService.getById(eventId) == null) {
            throw new ResourceNotFoundException("赛事不存在");
        }
        if (refereeService.getById(refereeId) == null) {
            throw new ResourceNotFoundException("裁判不存在");
        }
        if (!eventService.deleteReferee(new EventReferee(eventId, refereeId))) {
            throw new RuntimeException("删除裁判失败");
        }

    }

//    private void checkParams(Map<String, Long> params) {
//        params.forEach((k, v) -> {
//            if (v == null) {
//                throw new BadRequestException("传入的" + k + "为空");
//            }
//            if (services.get(k).getById(v) == null) {
//                throw new ResourceNotFoundException(k + "不存在");
//            }
//        });
//    }
}
