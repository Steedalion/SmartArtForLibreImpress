package org.libreimpress.smartart.layout;

import org.libreimpress.smartart.models.DiagramNode;
import org.libreimpress.smartart.models.DiagramType;

/** Maps a {@link DiagramType} to its layout algorithm. */
public final class LayoutFactory {

    private LayoutFactory() {}

    public static DiagramLayout build(DiagramType type, DiagramNode root) {
        switch (type) {
            case HUB_AND_SPOKE:      return HubAndSpokeLayout.layout(root);
            case PROCESS_FLOW:       return ProcessFlowLayout.layout(root);
            case SEQUENTIAL_CHEVRON: return SequentialChevronLayout.layout(root);
            case CYCLE:              return CycleLayout.layout(root);
            case CYCLE_ARROW:        return CycleArrowLayout.layout(root);
            case CYCLE_BLOCK:        return CycleBlockLayout.layout(root);
            case PYRAMID:            return PyramidLayout.layout(root);
            case BASIC_BLOCK_LIST:   return BlockListLayout.layout(root);
            case VERTICAL_BULLET_LIST: return VerticalBulletListLayout.layout(root);
            case BASIC_VENN:         return VennLayout.layout(root);
            case BASIC_MATRIX:       return MatrixLayout.layout(root);
            default:                 return HierarchyLayout.layout(root);
        }
    }
}
