// This file is part of OpenMVG, an Open Multiple View Geometry C++ library.

// Copyright (c) 2015 Pierre MOULON.

// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

#include "openMVG/sfm/sfm_data.hpp"
#include "openMVG/sfm/sfm_data_io.hpp"
#include "openMVG/types.hpp"
#include "SfMPlyHelper.hpp"

#include "openMVG/sfm/sfm_data_colorization.hpp"

#include <string>
#include <jni.h>

using namespace openMVG;
using namespace openMVG::sfm;

/// Export camera poses positions as a Vec3 vector
void GetCameraPositions(const SfM_Data & sfm_data, std::vector<Vec3> & vec_camPosition)
{
    for (const auto & view : sfm_data.GetViews())
    {
        if (sfm_data.IsPoseAndIntrinsicDefined(view.second.get()))
        {
            const geometry::Pose3 pose = sfm_data.GetPoseOrDie(view.second.get());
            vec_camPosition.push_back(pose.center());
        }
    }
}

// Convert from a SfM_Data format to another
extern "C" JNIEXPORT jstring JNICALL
Java_ir_sami_trowel_services_BuildModelTask_computeDataColor(
        JNIEnv *env,
        jobject obj, jstring jsSfM_Data_Filename_In, jstring jsOutputPLY_Out) {

    jboolean copy = true;
    std::string
            sSfM_Data_Filename_In = env->GetStringUTFChars(jsSfM_Data_Filename_In, &copy),
            sOutputPLY_Out = env->GetStringUTFChars(jsOutputPLY_Out, &copy);

    if (sOutputPLY_Out.empty())
    {
        std::cerr << std::endl
                  << "No output PLY filename specified." << std::endl;
        return env->NewStringUTF("FAILED");
    }

    // Load input SfM_Data scene
    SfM_Data sfm_data;
    if (!Load(sfm_data, sSfM_Data_Filename_In, ESfM_Data(ALL)))
    {
        std::cerr << std::endl
                  << "The input SfM_Data file \"" << sSfM_Data_Filename_In << "\" cannot be read." << std::endl;
        return env->NewStringUTF("FAILED");
    }

    // Compute the scene structure color
    std::vector<Vec3> vec_3dPoints, vec_tracksColor, vec_camPosition;
    if (ColorizeTracks(sfm_data, vec_3dPoints, vec_tracksColor))
    {
        GetCameraPositions(sfm_data, vec_camPosition);

        // Export the SfM_Data scene in the expected format
        if (plyHelper::exportToPly(vec_3dPoints, vec_camPosition, sOutputPLY_Out, &vec_tracksColor))
        {
            return env->NewStringUTF("SUCCESS");
        }
    }

    return env->NewStringUTF("FAILED");
}
