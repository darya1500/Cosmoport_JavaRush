package com.space.service;

import com.space.model.Ship;
import com.space.exception.*;
import com.space.model.ShipType;
import com.space.repository.ShipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.util.NestedServletException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class MainService implements ShipService {
    private ShipRepository shipRepository;

    @Autowired
    public void setShipRepository(ShipRepository shipRepository) {
        this.shipRepository = shipRepository;
    }

    @Override
    public Ship createShip(Ship ship) {
        checkShipParameters(ship);
        Double raiting = calculateRating(ship);
        ship.setRating(raiting);
        return shipRepository.saveAndFlush(ship);
    }

    private Double calculateRating(Ship ship) throws ShipNotFoundException {
        if (ship != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(ship.getProdDate());
            int year = cal.get(Calendar.YEAR);
            Boolean isUsed = ship.getUsed();
            if (isUsed == null) {
                isUsed = false;
                ship.setUsed(false);
            }
            BigDecimal raiting = BigDecimal.valueOf((80 * ship.getSpeed() * (isUsed ? 0.5 : 1)) / (3019 - year + 1));
            raiting = raiting.setScale(2, RoundingMode.HALF_UP);
            return raiting.doubleValue();
        } else {
            throw new ShipNotFoundException("Ship is null");
        }
    }

    private void checkShipParameters(Ship ship) {
        if (ship.getName() == null || ship.getName().isEmpty() || ship.getName().length() > 50) {
            throw new BadRequestException("Ship name is incorrect");
        }
        String planet = ship.getPlanet();
        if (planet == null) {
            throw new BadRequestException("Planet is incorrect");
        }
        if (planet.isEmpty()) {
            throw new BadRequestException("Planet is incorrect");
        }
        if (planet.length() > 50) {
            throw new BadRequestException("Planet is incorrect");
        }
        ShipType type = ship.getShipType();
        if (type == null) try {
            throw new ShipTypeException("Ship type is incorrect");
        } catch (ShipTypeException e) {
            e.printStackTrace();
        }
        Calendar cal = Calendar.getInstance();
        Date prodDate = ship.getProdDate();
        cal.setTime(ship.getProdDate());
        int year = cal.get(Calendar.YEAR);
        if (year < 2800 || year > 3019 || prodDate == null) {
            throw new BadRequestException("Production date is invalid");
        }
        Boolean isUsed = ship.getUsed();
        if (isUsed == null) {
            isUsed = false;
        }
        Double speed = ship.getSpeed();
        if (speed == null || speed < (0.01) || speed > (0.99)) {
            throw new BadRequestException("Speed is invalid");
        }
        Integer crewSize = ship.getCrewSize();
        if (crewSize == null||crewSize < 1 || crewSize > 9999) {
            throw new BadRequestException("Crew size is invalid");
        }
    }

    @Override
    public Page<Ship> gelAllShips(Specification<Ship> specification, Pageable sortedByName) {
        return shipRepository.findAll(specification, sortedByName);
    }

    @Override
    public List<Ship> gelAllShips(Specification<Ship> specification) {
        return shipRepository.findAll(specification);
    }

    @Override
    public Specification<Ship> filterByName(String name) {
        return (root, query, cb) -> name == null ? null : cb.like(root.get("name"), "%" + name + "%");
    }

    @Override
    public Specification<Ship> filterByPlanet(String planet) {
        return (root, query, cb) -> planet == null ? null : cb.like(root.get("planet"), "%" + planet + "%");
    }

    @Override
    public Specification<Ship> filterByDate(Long after, Long before) {
        return (root, query, cb) -> {
            if (after == null && before == null)
                return null;
            if (after == null) {
                Date before1 = new Date(before);
                return cb.lessThanOrEqualTo(root.get("prodDate"), before1);
            }
            if (before == null) {
                Date after1 = new Date(after);
                return cb.greaterThanOrEqualTo(root.get("prodDate"), after1);
            }
            Date before1 = new Date(before);
            Date after1 = new Date(after);
            return cb.between(root.get("prodDate"), after1, before1);
        };
    }

    @Override
    public Specification<Ship> filterBySpeed(Double min, Double max) {
        return (root, query, cb) -> {
            if (min == null && max == null)
                return null;
            if (min == null)
                return cb.lessThanOrEqualTo(root.get("speed"), max);
            if (max == null)
                return cb.greaterThanOrEqualTo(root.get("speed"), min);

            return cb.between(root.get("speed"), min, max);
        };
    }

    @Override
    public Specification<Ship> filterByCrewSize(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (min == null && max == null)
                return null;
            if (min == null)
                return cb.lessThanOrEqualTo(root.get("crewSize"), max);
            if (max == null)
                return cb.greaterThanOrEqualTo(root.get("crewSize"), min);

            return cb.between(root.get("crewSize"), min, max);
        };
    }

    @Override
    public Specification<Ship> filterByRating(Double min, Double max) {
        return (root, query, cb) -> {
            if (min == null && max == null)
                return null;
            if (min == null)
                return cb.lessThanOrEqualTo(root.get("rating"), max);
            if (max == null)
                return cb.greaterThanOrEqualTo(root.get("rating"), min);

            return cb.between(root.get("rating"), min, max);
        };
    }

    @Override
    public Specification<Ship> filterByUsage(Boolean isUsed) {
        return (root, query, cb) -> {
            if (isUsed == null)
                return null;
            if (isUsed)
                return cb.isTrue(root.get("isUsed"));
            else return cb.isFalse(root.get("isUsed"));
        };
    }

    @Override
    public Specification<Ship> filterByShipType(ShipType shipType) {
        return (root, query, cb) -> shipType == null ? null : cb.equal(root.get("shipType"), shipType);
    }

    @Override
    public void deleteById(Long id) {
        if (shipRepository.existsById(id)) {
            shipRepository.deleteById(id);
        } else throw new ShipNotFoundException("Ship not found");
    }

    @Override
    public Long checkAndParseId(String id) {
        if (id == null || id.equals(""))
            try {
                throw new InvalidIDException("ID is invalid");
            } catch (InvalidIDException e) {
                e.printStackTrace();
            }
        try {
            Long longId = Long.parseLong(id);
            if (longId == 0) {
                throw new BadRequestException("ID is invalid");
            }
            return longId;
        } catch (NumberFormatException e) {
            throw new BadRequestException("ID is invalid", e);
        }
    }

    @Override
    public Ship getShip(Long id) {
        if (shipRepository.existsById(id)) {
            return shipRepository.findById(id).get();
        } else throw new ShipNotFoundException("Ship is not found");
    }

    @Override
    public Ship editShip(Long id, Ship ship) {
        if (!shipRepository.existsById(id)) {
            throw new ShipNotFoundException("Ship was not found");
        }
        Ship oldShip = shipRepository.findById(id).get();
        checkShipParameters(oldShip);
        if (ship.getName() != null) {
            if (ship.getName() == null || ship.getName().isEmpty() || ship.getName().length() > 50) {
                throw new BadRequestException("Ship name is incorrect");
            }
            oldShip.setName(ship.getName());
        }
        if (ship.getPlanet() != null) {
            oldShip.setPlanet(ship.getPlanet());
        }
        if (ship.getProdDate() != null) {
            Calendar cal = Calendar.getInstance();
            Date prodDate = ship.getProdDate();
            cal.setTime(ship.getProdDate());
            int year = cal.get(Calendar.YEAR);
            if (year < 2800 || year > 3019 || prodDate == null) {
                throw new BadRequestException("Production date is invalid");
            }
            oldShip.setProdDate(ship.getProdDate());
        }
        if (ship.getSpeed() != null) {
            oldShip.setSpeed(ship.getSpeed());
        }
        if (ship.getCrewSize() != null) {
            Integer crewSize = ship.getCrewSize();
            if (crewSize == null||crewSize < 1 || crewSize > 9999) {
                throw new BadRequestException("Crew size is invalid");
            }
            oldShip.setCrewSize(ship.getCrewSize());
        }
        if (ship.getShipType() != null) {
            oldShip.setShipType(ship.getShipType());
        }
        if (ship.getUsed() != null) {
            oldShip.setUsed(ship.getUsed());
            if (ship.getRating() != null) {
        }
            oldShip.setRating(ship.getRating());
        }
        return shipRepository.save(oldShip);
    }
}
