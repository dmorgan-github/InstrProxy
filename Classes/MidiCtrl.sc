/*
(
if ( 'MidiCtrl3'.asClass.notNil ) {
    \here.postln;
} {
    \there.postln
}
)
*/

MidiCtrl {

    classvar <devices;
    var <node;

    *new {|node|
        ^super.new.prInit(node);
    }

    *connect {|device, cb|

        var dest, src, obj, out;
        // MIDIClient.list is async
        fork({
            await {|done| MIDIClient.list; fork({ 
                "connect to %".format(device).inform;
                1.wait; 
                done.value(\ok) 
            }) };

            dest = MIDIClient.destinations.detectIndex({|val| val.device.toLower.contains(device.asString) });
            if (dest.isNil) {
                "dest not found %".format(device).throw
            };
            out = MIDIOut(dest).connect;

            src = MIDIClient.sources.detectIndex({|val| val.device.toLower.contains(device.asString) });
            if (src.isNil) {
                "src not found %".format(device).throw
            };
            MIDIIn.connect(src, MIDIClient.sources[src]);

            obj = (src: MIDIClient.sources[src], dest: MIDIClient.destinations[dest], out:out);
            devices.put(device.asSymbol, obj);
            cb.(obj);
        })
    }

    note {|device, noteChan, note, debug=false|
        var key = node.key;
        MidiCtrl.note(key, device, node, noteChan, note, debug);
    }

    *note {|key, device, node, noteChan, note, debug=false|

        var func, obj;
        var noteonkey = "%_noteon".format(key).asSymbol;
        var noteoffkey = "%_noteoff".format(key).asSymbol;

        if (note.isNil) {
            note = (0..110);
        };

        func = {|obj|

            var srcId;
            srcId = obj['src'].uid.debug("src uid");

            MIDIdef.noteOn(noteonkey.debug("noteonkey"), {|vel, note, chan|
                this.node.on(note, vel, debug:debug);
            }, noteNum:note, chan:noteChan, srcID: srcId)
            .fix;

            MIDIdef.noteOff(noteoffkey.debug("noteoffkey"), {|vel, note, chan|
                this.node.off(note);
            }, noteNum:note, chan:noteChan, srcID: srcId)
            .fix;
        };

        obj = MidiCtrl.devices.at(device.asSymbol).debug("device");
        if (obj.isNil) {
            MidiCtrl.connect(device, cb:{|val| func.(obj)  });
        } {
            func.(obj)
        }
    }

    cc {|device, pairs, ccChan=0|
        var key = node.key;
        MidiCtrl.cc(key, device, node, pairs, ccChan);
    }

    *cc {|key, device, node, pairs, ccChan=0|

        var cckey = "%_cc_%_%".format(key, ccChan, device).asSymbol.debug("mididef");

        if (pairs.isNil) {
            cckey.debug("disconnect");
            MIDIdef.cc(cckey).permanent_(false).free;
        }{
            var props, ccNums;
            var order, func, obj;

            props = pairs.select({|a, i| i.even});
            ccNums = pairs.select({|a, i| i.odd});

            func = {|order, obj|

                var srcId, out;
                srcId = obj['src'].uid.debug("src uid");
                out = obj['out'].debug("out");

                MIDIdef.cc(cckey, {|val, num, chan|
                    var mapped, ctrl, spec, filter;
                    ctrl = order[num];
                    spec = node.getSpec[ctrl];
                    if (spec.isNil) {
                        spec = [0, 1].asSpec;
                    };
                    mapped = spec.map(val/127);
                    //[ctrl, mapped, val, ].postln;
                    node.set(ctrl, mapped);
                }, ccNum:ccNums, chan:ccChan, srcID:srcId)
                .fix;

                order.indices.do({|num|
                    var ctrl = order[num];
                    var spec = node.getSpec[ctrl];
                    var min, max, current, ccval;
                    if (spec.isNil) {
                        spec = [0, 1].asSpec;
                    };

                    min = spec.minval;
                    max = spec.maxval;
                    current = node.get(ctrl);
                    if (current.notNil) {
                        // don't know how to unmap to a range that is not 0-1
                        if (spec.warp.isKindOf(ExponentialWarp)) {
                            ccval = current.explin(min, max, 0, 127);
                        }{
                            ccval = current.linlin(min, max, 0, 127);
                        };
                        //[node.key, \curent, current, \cc, ccval].debug(ctrl);
                        try {
                            out.control(ccChan, num, ccval);
                        } {|err|
                            "midi out: %".format(err).warn;
                        }
                    }
                });
            };

            order = Order.newFromIndices(props.asArray, ccNums.asArray);
            obj = MidiCtrl.devices.at(device.asSymbol).debug("device");
            if (obj.isNil) {
                MidiCtrl.connect(device, cb:{|val| func.(order, val)  });
            } {
                func.(order, obj)
            }
        }
    }

    *trace {arg enable=true;
        MIDIFunc.trace(enable);
    }

    *connectAll {
        MIDIClient.init(verbose:true);
        MIDIIn.connectAll(verbose:true);
    }

    prInit {|argNode|
        node = argNode;
    }

    *initClass {
        devices = Dictionary();
        MIDIClient.init(verbose:true);
    }
}


