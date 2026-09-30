//@ ImagePlus myImp
//@ Boolean (label="ALLOW_TRACK_SPLITTING", value=false) allow_track_splitting
//@ Double (label="GAP_CLOSING_MAX_DISTANCE", value = 200) gap_closing_max_distance
//@ Double (label="LINKING_MAX_DISTANCE", value=200) linking_max_distance
//@ Integer (label="MAX_FRAME_GAP", value=3) max_frame_gap

import fiji.plugin.trackmate.Logger
import fiji.plugin.trackmate.Model
import fiji.plugin.trackmate.Settings
import fiji.plugin.trackmate.TrackMate
import fiji.plugin.trackmate.action.LabelImgExporter
import fiji.plugin.trackmate.detection.LabelImageDetectorFactory
import fiji.plugin.trackmate.tracking.jaqaman.LAPUtils
import fiji.plugin.trackmate.tracking.jaqaman.SimpleSparseLAPTrackerFactory
import ij.IJ
import ij.ImagePlus

Double linking_max_distance = linking_max_distance
Double gap_closing_max_distance = gap_closing_max_distance
Boolean allow_track_splitting = allow_track_splitting
Integer max_frame_gap = max_frame_gap
ImagePlus myImp = myImp

ImagePlus myImp2 = TrackMateMe.run(myImp, linking_max_distance, gap_closing_max_distance, allow_track_splitting, max_frame_gap)
myImp2.show()

class TrackMateMe {
    static ImagePlus run(ImagePlus myImp, linking_max_distance, gap_closing_max_distance, allow_track_splitting, max_frame_gap) {
        // Swap Z and T dimensions if T=1
        def dims = myImp.getDimensions() // default order: XYCZT
        if (dims[4] == 1) {
            IJ.log('Flipping dimensions Z<->T')
            myImp.setDimensions(dims[2], dims[4], dims[3])
        }
        def model = new Model()
        model.setLogger(Logger.IJ_LOGGER)
        def settings = new Settings(myImp)
        settings.detectorFactory = new LabelImageDetectorFactory()
        //   settings.trackerFactory = new SparseLAPTrackerFactory()
        settings.trackerFactory = new SimpleSparseLAPTrackerFactory()
        settings.trackerSettings = LAPUtils.getDefaultSegmentSettingsMap()
        settings.trackerSettings['ALLOW_GAP_CLOSING'] = true
        settings.trackerSettings['ALLOW_TRACK_MERGING'] = false
        settings.trackerSettings['ALLOW_TRACK_SPLITTING'] = allow_track_splitting
        //    settings.trackerSettings['ALTERNATIVE_LINKING_COST_FACTOR']
        //    settings.trackerSettings['BLOCKING_VALUE']
        //    settings.trackerSettings['CUTOFF_PERCENTILE']
        //    settings.trackerSettings['GAP_CLOSING_FEATURE_PENALTIES']
        settings.trackerSettings['GAP_CLOSING_MAX_DISTANCE'] = gap_closing_max_distance
        //    settings.trackerSettings['GAP_CLOSING_MAX_FRAME_GAP']
        //    settings.trackerSettings['LINKING_FEATURE_PENALTIES']
        settings.trackerSettings['LINKING_MAX_DISTANCE'] = linking_max_distance
        settings.trackerSettings['MAX_FRAME_GAP'] = max_frame_gap
        //    settings.trackerSettings['MERGING_FEATURE_PENALTIES']
        //    settings.trackerSettings['MERGING_MAX_DISTANCE']
        //    settings.trackerSettings['SPLITTING_FEATURE_PENALTIES']
        //    settings.trackerSettings['SPLITTING_MAX_DISTANCE']
        def error = new StringBuilder()
        LAPUtils.checkSettingsValidity(settings.trackerSettings, error, false)

        def detectorSettings = [
                'TARGET_CHANNEL'   : 1,
                'SIMPLIFY_CONTOURS': false
        ]
        settings.detectorSettings = detectorSettings.asImmutable()
        def trackmate = new TrackMate(model, settings)
        def ok = trackmate.checkInput()
        if (!ok) {
            model.getLogger().log(trackmate.getErrorMessage())
            return null
        }
        ok = trackmate.process()
        if (!ok) {
            model.getLogger().log(trackmate.getErrorMessage())
            return null
        }
        //----------------
        // export LabelImagePlus
        //    Parameter TARGET_CHANNEL could not be found in settings map, or is null.
        //    Parameter SIMPLIFY_CONTOURS could not be found in settings map, or is null.
        //    Mandatory key TARGET_CHANNEL was not found in the map.
        //    Mandatory key SIMPLIFY_CONTOURS was not found in the map.

        //----------------
        def labelImgExporter = new LabelImgExporter()
        def outimg = labelImgExporter.createLabelImagePlus(trackmate.getModel(), myImp, false, false, LabelImgExporter.LabelIdPainting.LABEL_IS_TRACK_ID)
        IJ.run(outimg, 'glasbey_on_dark', '')
        IJ.setMinAndMax(outimg, 0, 255)
        outimg.setTitle(myImp.getTitle() + '_tracked')
        return outimg
    }
}
