package technology.rocketjump.saul.entities.model.physical.creature.body;

import org.apache.commons.lang3.EnumUtils;

import java.util.Iterator;
import java.util.Stack;

class BodyPartIterator implements Iterator<BodyPart> {
    private final Body body;
    private final Stack<BodyPart> frontier = new Stack<>();


    BodyPartIterator(Body body, BodyPart rootPart) {
        this.body = body;
        includeBodyPart(rootPart);
    }

    @Override
    public boolean hasNext() {
        return !frontier.isEmpty();
    }

    @Override
    public BodyPart next() {
        BodyPart iteration = frontier.pop();
        BodyPartDiscriminator parentDiscriminator = iteration.getDiscriminator();

        for (String childPartName : iteration.getPartDefinition().getChildParts()) {
            BodyPartDefinition specificDefinition = body.getBodyStructure().getPartDefinitionByName(childPartName).orElse(null);

            if (specificDefinition == null) {
                BodyPartDiscriminator childDiscriminator = null;

                String[] split = childPartName.split("-");
                if (split.length > 1) {
                    childDiscriminator = EnumUtils.getEnum(BodyPartDiscriminator.class, split[0]);
                    childPartName = split[1];
                }

                BodyPartDefinition genericDefinition = body.getBodyStructure().getPartDefinitionByName(childPartName).orElse(null);
                if (childDiscriminator == null) {
                    childDiscriminator = parentDiscriminator;
                }

                includeBodyPart(new BodyPart(genericDefinition, childDiscriminator));
            } else {
                includeBodyPart(new BodyPart(specificDefinition, parentDiscriminator));
            }
        }

        return iteration;
    }

    private void includeBodyPart(BodyPart part) {
        frontier.push(part);
    }

}
