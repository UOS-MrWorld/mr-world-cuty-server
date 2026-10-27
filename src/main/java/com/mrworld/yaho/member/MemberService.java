package com.mrworld.yaho.member;

import com.mrworld.yaho.common.exception.BadRequestException;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.member.dto.MemberInfoResponse;
import com.mrworld.yaho.member.dto.MemberUpdateRequest;
import com.mrworld.yaho.member.dto.MemberUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberInfoResponse getMyInfo(Long memberId) {
        return MemberInfoResponse.from(findMember(memberId));
    }

    // 전화번호와 주소 중 보낸 값만 바꾼다.
    @Transactional
    public MemberUpdateResponse updateMyInfo(Long memberId, MemberUpdateRequest request) {
        if (request.getPhoneNum() == null && request.getAddress() == null) {
            throw new BadRequestException("수정할 값이 없습니다.");
        }

        Member member = findMember(memberId);
        member.updateProfile(request.getPhoneNum(), request.getAddress());
        return MemberUpdateResponse.from(member);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 회원입니다."));
    }
}
