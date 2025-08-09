package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.AreaDTO;
import ahqpck.maintenance.report.dto.RoleDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.entity.Area;
import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.exception.NotFoundException;
import ahqpck.maintenance.report.repository.AreaRepository;
import ahqpck.maintenance.report.repository.UserRepository;
import ahqpck.maintenance.report.specification.AreaSpecification;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaRepository areaRepository;
    private final UserRepository userRepository;
    private final Validator validator;

    public Page<AreaDTO> getAllAreas(String keyword, int page, int size, String sortBy, boolean asc) {
        Sort sort = asc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Area> spec = AreaSpecification.search(keyword);
        Page<Area> areaPage = areaRepository.findAll(spec, pageable);

        return areaPage.map(this::toDTO);
    }

    public AreaDTO getAreaById(String id) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Area not found with ID: " + id));
        return toDTO(area);
    }

    public void createArea(AreaDTO dto) {
        if (areaRepository.existsByCodeIgnoreCase(dto.getCode())) {
            throw new IllegalArgumentException("Area with this code already exists.");
        }

        Area area = new Area();
        mapToEntity(area, dto);
        areaRepository.save(area);
    }

    public void updateArea(AreaDTO dto) {
        Area area = areaRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("Area not found with ID: " + dto.getId()));

        mapToEntity(area, dto);
        areaRepository.save(area);
    }

    public void deleteArea(String id) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Area not found with ID: " + id));

        areaRepository.delete(area);
    }

    private void mapToEntity(Area area, AreaDTO dto) {
        area.setCode(dto.getCode().trim());
        area.setName(dto.getName().trim());
        area.setStatus(dto.getStatus());
        area.setDescription(dto.getDescription());

        // Map responsiblePerson
        if (dto.getResponsiblePerson() != null && dto.getResponsiblePerson().getId() != null) {
            String userId = dto.getResponsiblePerson().getId();
            area.setResponsiblePerson(userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId)));
        } else {
            throw new IllegalArgumentException("Responsible person is required");
        }
    }

    private UserDTO mapToUserDTO(User user) {
        if (user == null)
            return null;

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setEmail(user.getEmail());
        return dto;
    }

    private AreaDTO toDTO(Area area) {
        AreaDTO dto = new AreaDTO();
        dto.setId(area.getId());
        dto.setCode(area.getCode());
        dto.setName(area.getName());
        dto.setStatus(area.getStatus());
        dto.setDescription(area.getDescription());

        dto.setResponsiblePerson(mapToUserDTO(area.getResponsiblePerson()));

        return dto;
    }
}