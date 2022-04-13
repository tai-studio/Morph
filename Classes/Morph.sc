Morph {
	var <deviceID;
	var <>binary;
	var <oscResponders;
	var <deviceSpecs;
	var <>avgAction, <>syncAction;
	var <>contactPreAction,<>contactPostAction;
	var <>contactDownAction, <>contactUpAction, <>contactDragAction;
	var <deviceVals, <vals, <devicePrevVals, <prevVals;
	var <contactProto, <contactDeviceProto, <avgProto, <updatedContacts, <maxContacts=16;
	var gui;

	*new{|deviceID = 0|
		^super.newCopyArgs(deviceID).init;
	}

	contacts {
		^(vals.contacts)
	}
	avg {
		^(vals.avg)
	}
	init {
		binary = "~/vol/dev/interfacing/senselosc/build/apps/senselosc".standardizePath;

		deviceSpecs = (
			\state      : ReverseIdxItemsSpec([\invalid, \start, \drag, \end], 0), // in lfsaw.de quark
			\numContacts: [0, maxContacts-1, \lin].asSpec,
			\x          : [0,    240,      \lin].asSpec,
			\y          : [0,    139,      \lin].asSpec,
			\wX         : [0,    240,      \lin].asSpec,
			\wY         : [0,    139,      \lin].asSpec,
			\dX         : [0,    240,      \lin].asSpec,
			\dY         : [0,    139,      \lin].asSpec,
			\minX       : [0,    240,      \lin].asSpec,
			\minY       : [0,    139,      \lin].asSpec,
			\maxX       : [0,    240,      \lin].asSpec,
			\maxY       : [0,    139,      \lin].asSpec,
			\peakX      : [0,    240,      \lin].asSpec,
			\peakY      : [0,    139,      \lin].asSpec,
			//// "correct" values
			// \force      : [0,   8192,      \lin].asSpec,
			// \peakForce  : [0,   8192,      \lin].asSpec,
			// \tForce     : [0, 3*8192,      \lin].asSpec,
			// \dForce     : [0, 3*8192,      \lin].asSpec,
			// \area : [0, 33360, \lin].asSpec,
			\force      : [0,    0.25 * 8192, \lin].asSpec,
			\peakForce  : [0,    0.25 * 8192, \lin].asSpec,
			\dForce     : [0,    0.25 * 8192, \lin].asSpec,
			\tForce     : [0, 3* 0.25 * 8192, \lin].asSpec,
			\area       : [0, 33360 * 0.2, \lin].asSpec,
			\dArea      : [0, 33360 * 0.2, \lin].asSpec,
			\dist       : [0, 240, \lin].asSpec,
			\wDist      : [0, 240, \lin].asSpec,
			\orientation: [0, 360, \lin].asSpec,
			\majAxis    : [0, 240, \lin].asSpec,
			\minAxis    : [0, 240, \lin].asSpec
		);

		contactDeviceProto = (
			state:        0,
			x:            0,
			y:            0,
			force:        0,
			area:         0,
			dist:         0,
			wDist:        0,
			orientation:  0,
			majAxis:      0,
			minAxis:      0,
			peakX:        0,
			peakY:        0,
			peakForce:    0,
			minX:         0,
			minY:         0,
			maxX:         0,
			maxY:         0,
			dX:           0,
			dY:           0,
			dForce:       0,
			dArea:        0
		);

		contactProto = contactDeviceProto.collect{|v, k|
			deviceSpecs[k].unmap(v)
		};

		avgProto = (
			numContacts: 0,
			x: 0,
			y: 0,
			force: 0,
			dist: 0,
			area: 0,
			wX: 0,
			wY: 0,
			tForce: 0,
			wDist: 0,
		);

		updatedContacts = ();

		vals = (
			contacts: {().proto_(contactProto)}!maxContacts,  // contacts
			avg: ().proto_(avgProto); // average values
		);
		// prevVals = (
		// 	c: {().proto_(contactProto)}!maxContacts,  // contacts
		// 	avg: ().proto_(avgProto); // average values
		// );
		deviceVals = (
			contacts: {().proto_(contactDeviceProto)}!maxContacts,  // contacts
			avg: ().proto_(avgProto); // average values
		);
		// devicePrevVals = (
		// 	c: {().proto_(contactProto)}!maxContacts,  // contacts
		// 	avg: ().proto_(avgProto); // average values
		// );

		// all responders
		oscResponders = ();
	}

	runBinary {|scanDetail = 2|
		var sendPort = NetAddr.localAddr.port;
		var command = format("% -d% -p%", binary, scanDetail, sendPort);
		command.postln.runInTerminal;
	}
	start {|force = false|
		this.makeResponders(force);
	}
	stop {
		this.freeResponders;
	}
	makeResponders {|force = false|
		force.if({
			this.freeResponders;
			}, {
				// don't do if already there
				oscResponders.notEmpty.if({
					^this
			});
		});

		oscResponders = (
			\avg: OSCFunc({|msg, time|
				var myVals, myPrevVals, myDeviceVals, myDevicePrevVals;
				var name, deviceID, numContacts, x, y, force, dist, area, wX, wY, tForce, wDist;
				# name, deviceID, numContacts, x, y, force, dist, area, wX, wY, tForce, wDist = msg;

				// prevVals       = vals.avg.deepCopy;
				// devicePrevVals = deviceVals.avg.deepCopy;

				myVals     = vals.avg;
				myDeviceVals = deviceVals.avg;

				myVals.putPairs([
					\numContacts,  deviceSpecs[\numContacts].unmap(numContacts),
					\x,            deviceSpecs[\x          ].unmap(x),
					\y,            deviceSpecs[\y          ].unmap(y),
					\force,        deviceSpecs[\force      ].unmap(force),
					\dist,         deviceSpecs[\dist       ].unmap(dist),
					\area,         deviceSpecs[\area       ].unmap(area),
					\wX,           deviceSpecs[\wX         ].unmap(wX),
					\wY,           deviceSpecs[\wY         ].unmap(wY),
					\tForce,       deviceSpecs[\tForce     ].unmap(tForce),
					\wDist,        deviceSpecs[\wDist      ].unmap(wDist),
					]);

				myDeviceVals.putPairs([
					\numContacts, numContacts,
					\x, x,
					\y, y,
					\force, force,
					\dist, dist,
					\area, area,
					\wX, wX,
					\wY, wY,
					\tForce, tForce,
					\wDist, wDist,
					]);
				}, "/contactAvg", argTemplate: [deviceID]),

			\contact: OSCFunc({|msg, time|
				var myVals, myPrevVals, myDeviceVals, myDevicePrevVals;
				var name, deviceID, id, state, x, y, force, area, dist, wDist, orientation, majAxis, minAxis;
				# name, deviceID, id, state, x, y, force, area, dist, wDist, orientation, majAxis, minAxis = msg;

				// prevVals      [id] = vals.contacts[id].deepCopy;
				// devicePrevVals[id] = deviceVals.contacts[id].deepCopy;

				myVals     = vals.contacts[id];
				myDeviceVals = deviceVals.contacts[id];

				myVals.putPairs([
					\state,       deviceSpecs[\state      ].unmap(state),
					\x,           deviceSpecs[\x          ].unmap(x),
					\y,           deviceSpecs[\y          ].unmap(y),
					\force,       deviceSpecs[\force      ].unmap(force),
					\area,        deviceSpecs[\area       ].unmap(area),
					\dist,        deviceSpecs[\dist       ].unmap(dist),
					\wDist,       deviceSpecs[\dist       ].unmap(wDist),
					\orientation, deviceSpecs[\orientation].unmap(orientation),
					\majAxis,     deviceSpecs[\majAxis    ].unmap(majAxis),
					\minAxis,     deviceSpecs[\minAxis    ].unmap(minAxis),
					]);

				myDeviceVals.putPairs([
					\state,       state,
					\x,           x,
					\y,           y,
					\force,       force,
					\area,        area,
					\dist,        dist,
					\wDist,       wDist,
					\orientation, orientation,
					\majAxis,     majAxis,
					\minAxis,     minAxis,
					]);


				}, "/contact", argTemplate: [deviceID]),

			\delta: OSCFunc({|msg, time| // delta
				var myVals, myPrevVals, myDeviceVals, myDevicePrevVals;
				var name, deviceID, id, state, dX, dY, dForce, dArea;
				# name, deviceID, id, state, dX, dY, dForce, dArea = msg;

				// prevVals      [id] = vals.contacts[id].deepCopy;
				// devicePrevVals[id] = deviceVals.contacts[id].deepCopy;

				myVals     = vals.contacts[id];
				myDeviceVals = deviceVals.contacts[id];

				myVals.putPairs([
					\dX,     deviceSpecs[\x].unmap(dX),
					\dY,     deviceSpecs[\y].unmap(dY),
					\dForce, deviceSpecs[\force].unmap(dForce),
					\dArea,  deviceSpecs[\area].unmap(dArea),
					]);

				myDeviceVals.putPairs([
					// \state,    state,
					\dX,     dX,
					\dY,     dY,
					\dForce, dForce,
					\dArea,  dArea,
					]);
				}, "/contactDelta", argTemplate: [deviceID]),

			\bb: OSCFunc({|msg, time| // BoundingBox
				var myVals, myPrevVals, myDeviceVals, myDevicePrevVals;
				var name, deviceID, id, state, minX, minY, maxX, maxY;
				# name, deviceID, id, state, minX, minY, maxX, maxY = msg;

				// prevVals      [id] = vals.contacts[id].deepCopy;
				// devicePrevVals[id] = deviceVals.contacts[id].deepCopy;

				myVals     = vals.contacts[id];
				myDeviceVals = deviceVals.contacts[id];

				myVals.putPairs([
					\minX,       deviceSpecs[\minX].unmap(minX),
					\minY,       deviceSpecs[\minY].unmap(minY),
					\maxX,       deviceSpecs[\maxX].unmap(maxX),
					\maxY,       deviceSpecs[\maxY].unmap(maxY),
					]);

				myDeviceVals.putPairs([
					// \state,    state,
					\minX, minX,
					\minY, minY,
					\maxX, maxX,
					\maxY, maxY,
					]);
				}, "/contactBB", argTemplate: [deviceID]),

			\peak: OSCFunc({|msg, time| // peak
				var myVals, myPrevVals, myDeviceVals, myDevicePrevVals;
				var name, deviceID, id, state, peakX, peakY, peakForce;
				# name, deviceID, id, state, peakX, peakY, peakForce = msg;

				// prevVals      [id] = vals.contacts[id].deepCopy;
				// devicePrevVals[id] = deviceVals.contacts[id].deepCopy;

				myVals     = vals.contacts[id];
				myDeviceVals = deviceVals.contacts[id];

				myVals.putPairs([
					\peakX,      deviceSpecs[\peakX].unmap(peakX),
					\peakY,      deviceSpecs[\peakY].unmap(peakY),
					\peakForce,  deviceSpecs[\peakForce].unmap(peakForce),
					]);

				myDeviceVals.putPairs([
					\peakX, peakX,
					\peakY, peakY,
					\peakForce, peakForce,
					]);
				}, "/contactPeak", argTemplate: [deviceID]),

			\sync: OSCFunc({|msg, time| // sync
				var name, deviceID, updatedContactIDs;
				var myVals, myPrevVals, myDeviceVals, myDevicePrevVals;

				# name, deviceID ... updatedContactIDs  = msg;

				updatedContactIDs = updatedContactIDs.selectIndices{|v| v > 0}; // convert to indices
				updatedContacts = updatedContactIDs.collectAs({|id| (id -> vals.contacts[id])}, Event);

				// average action
				avgAction.value(vals.avg, this);

				// contact actions
				updatedContactIDs.do{|id|
					myVals     = vals.contacts[id];
					myDeviceVals = deviceVals.contacts[id];

					contactPreAction.value(myVals, id, this);
					switch(myDeviceVals.state,
						0, {
							"%(%): invalid drag received\n%".format(this.class.name, this.deviceID, myVals).warn;
						},
						1, {
							contactDownAction.value(myVals, id, this);
						},
						2, {
							contactDragAction.value(myVals, id, this);
						},
						3, {
							contactUpAction.value(myVals, id, this);
						}
					);
					contactPostAction.value(myVals, id, this);
				};

				// sync action
				syncAction.value(
					vals.avg,
					updatedContacts,
					this
				);

				gui.notNil.if{
					{gui.window.refresh}.defer;
				}

			}, "sync", argTemplate: [deviceID]),
		)
	}

	clearActions {
		contactPreAction  = nil;
		contactPostAction = nil;
		contactDownAction = nil;
		contactUpAction   = nil;
		contactDragAction = nil;
	}

	freeResponders {
		oscResponders.do{|resp|
			resp.free;
		};
		oscResponders = ();
	}

	gui {|bgColor, cColor, avgColor, wavgColor|
		gui.isNil.if{
			gui = MorphGUI(this, bgColor, cColor, avgColor, wavgColor);
		}
		^gui;
	}
	guiClosed {
		gui = nil;
	}
}


MorphGUI {
	var <morph, <bgColor, <>cColor, <>avgColor, <>wavgColor;
	var <window, view;
	*new {|morph, bgColor, cColor, avgColor, wavgColor|
		^super.newCopyArgs(morph, bgColor, cColor, avgColor, wavgColor).init;
	}
	bgColor_{|color|
		bgColor = color;
		view.background_(bgColor);
	}
	init{
		bgColor = bgColor ?? {Color.gray(0.5)};
		cColor = cColor ?? {Color.gray(1, 1)};
		avgColor = avgColor ?? {Color.gray(1, 0.3)};
		wavgColor = wavgColor ?? {Color.gray(1, 0.5)};

		window = Window("Morph (%)".format(morph.deviceID), 400@300).front;
		window.onClose_{
			morph.guiClosed
		};
		view = UserView(window, window.view.bounds.insetBy(20,20));
		view
			.resize_(5)
			.background_(bgColor);

		view.drawFunc = {|view|
			var width, height;

			var avgPos = [morph.avg.x, morph.avg.y];
			var avgWPos = [morph.avg.wX, morph.avg.wY];
			var avgForce = morph.avg.force;
			var avgTForce = morph.avg.tForce;
			var extent = [view.bounds.width, view.bounds.height];
			var scaling = extent.maxItem;
			morph.updatedContacts.keysValuesDo({|k, me|
				var pos = [me.x, me.y];
				var force = me.force;

				Pen.color = cColor;
				Pen.addOval(Rect.aboutPoint((pos * extent).asPoint, force * scaling, force * scaling));
				Pen.fill;
				Pen.color = avgColor;
				Pen.line((pos * extent).asPoint, (avgPos * extent).asPoint);
				Pen.stroke;
				Pen.color = wavgColor;
				Pen.line((pos * extent).asPoint, (avgWPos * extent).asPoint);
				Pen.stroke;
			});

			// avg
			Pen.color = avgColor;
			Pen.addOval(Rect.aboutPoint((avgPos * extent).asPoint, avgForce * scaling, avgForce * scaling));
			Pen.fill;

			// weighted avg
			Pen.color = wavgColor;
			Pen.addOval(Rect.aboutPoint((avgWPos * extent).asPoint, avgTForce * scaling, avgTForce * scaling));
			Pen.fill;
		};
	}
}
