package com.dozingcatsoftware.vectorpinball.elements;

import static com.dozingcatsoftware.vectorpinball.util.MathUtils.asFloat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.IFieldRenderer;
import com.dozingcatsoftware.vectorpinball.model.Point;

/**
 * This FieldElement subclass is used to identify areas on the table that should cause custom behavior
 * when the ball enters. SensorElements have no bodies and don't draw anything. The area they monitor
 * can be a rectangle defined by the "rect" parameter as a [xmin,ymin,xmax,ymax] list, or a circle defined
 * by the "center" and "radius" parameters. During every tick() invocation, a sensor determines if any of
 * the field's balls are within its area, and if so calls the field delegate's ballInSensorRange method.
 * @author brian
 *
 */

public class SensorElement extends FieldElement {
    public static final String CENTER_PROPERTY = "center";
    public static final String RADIUS_PROPERTY = "radius";
    public static final String RECT_PROPERTY = "rect";

	float xmin, ymin, xmax, ymax;
	boolean circular = false;
	float cx, cy; // center for circular areas
	float radius, radiusSquared;

	@Override public void finishCreateElement(Map params, FieldElementCollection collection) {
		if (params.containsKey(CENTER_PROPERTY) && params.containsKey(RADIUS_PROPERTY)) {
			this.circular = true;
			List centerPos = (List)params.get(CENTER_PROPERTY);
			this.cx = asFloat(centerPos.get(0));
			this.cy = asFloat(centerPos.get(1));
			this.radius = asFloat(params.get(RADIUS_PROPERTY));
			this.radiusSquared = radius*radius;
			// create bounding box to allow rejecting balls without making distance calculations
			this.xmin = this.cx - radius/2;
			this.xmax = this.cx + radius/2;
			this.ymin = this.cy - radius/2;
			this.ymax = this.cy + radius/2;
		}
		else {
			List rectPos = (List)params.get(RECT_PROPERTY);
			this.xmin = asFloat(rectPos.get(0));
			this.ymin = asFloat(rectPos.get(1));
			this.xmax = asFloat(rectPos.get(2));
			this.ymax = asFloat(rectPos.get(3));
		}
	}

	@Override public void createBodies(World world) {
	    // Not needed.
	}

	@Override public boolean shouldCallTick() {
		return true;
	}

	boolean ballInRange(Body ball) {
		Vector2 bpos = ball.getPosition();
		// test against rect
		if (bpos.x < xmin || bpos.x > xmax || bpos.y < ymin || bpos.y > ymax) {
			return false;
		}
		// if circle, test (squared) distance to center
		if (this.circular) {
			float distSquared = (bpos.x-this.cx) * (bpos.x-this.cx) + (bpos.y-this.cy)*(bpos.y-this.cy);
			if (distSquared > this.radiusSquared) return false;
		}
		return true;
	}

	@Override public void tick(Field field) {
		List<Body> balls = field.getBalls();
		for(int i=0; i<balls.size(); i++) {
			Body ball = balls.get(i);
			if (ballInRange(ball)) {
				field.getDelegate().ballInSensorRange(field, this, ball);
				return;
			}
		}
	}

	@Override public List<Body> getBodies() {
		return Collections.EMPTY_LIST;
	}

	@Override public void draw(IFieldRenderer renderer) {
		// no UI
	}

	// Editor support.
	static final Color EDITOR_OUTLINE_COLOR = Color.fromRGB(128, 128, 128);

	@Override public void drawForEditor(IFieldRenderer renderer, boolean isSelected) {
	    // Show active area for editor.
	    if (circular) {
	        renderer.frameCircle(cx, cy, (float)Math.sqrt(radiusSquared), EDITOR_OUTLINE_COLOR);
	    }
	    else {
            renderer.drawLine(xmin, ymin, xmax, ymin, EDITOR_OUTLINE_COLOR);
            renderer.drawLine(xmax, ymin, xmax, ymax, EDITOR_OUTLINE_COLOR);
            renderer.drawLine(xmax, ymax, xmin, ymax, EDITOR_OUTLINE_COLOR);
            renderer.drawLine(xmin, ymax, xmin, ymin, EDITOR_OUTLINE_COLOR);
	    }
	    if (isSelected) {
	        // TODO: indicate selection
	    }
	}

    @Override public boolean isPointWithinDistance(Point point, double distance) {
        // Always treat as rectangle.
        double centerX = (xmin + xmax) / 2;
        double centerY = (ymin + ymax) / 2;
        double actualDist = Math.max(Math.abs(point.x-centerX), Math.abs(point.y-centerY));
        return actualDist <= distance;
    }

    @Override public void handleDrag(Point point, Point deltaFromStart, Point deltaFromPrevious) {
        if (circular) {
            cx += deltaFromPrevious.x;
            cy += deltaFromPrevious.y;
        }
        else {
            xmin += deltaFromPrevious.x;
            ymin += deltaFromPrevious.y;
            xmax += deltaFromPrevious.x;
            ymax += deltaFromPrevious.y;
        }
    }

    @Override public Map<String, Object> getPropertyMap() {
        Map<String, Object> properties = mapWithDefaultProperties();
        if (circular) {
            properties.put(CENTER_PROPERTY, Arrays.asList(cx, cy));
            properties.put(RADIUS_PROPERTY, radius);
        }
        else {
            properties.put(RECT_PROPERTY, Arrays.asList(xmin, ymin, xmax, ymax));
        }
        return properties;
    }
}
