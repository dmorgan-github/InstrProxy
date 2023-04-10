MidiCtrl {

    var <node;

    *new {|node|
        ^super.new.prInit(node);
    }

    // TODO: ccChan bookkeeping
    cc {|props, ccNums, ccChan=0|

        var key = this.node.key;
        var cckey = "%_cc_%".format(key, ccChan).asSymbol.debug("mididef");

        if (props.isNil) {
            cckey.debug("disconnect");
            MIDIdef.cc(cckey).permanent_(false).free;
        }{
            var order = Order.newFromIndices(props.asArray, ccNums.asArray);
            MIDIdef.cc(cckey, {|val, num, chan|
                var mapped, ctrl, spec, filter;
                ctrl = order[num];
                spec = node.getSpec(ctrl);
                if (spec.isNil) {
                    spec = [0, 1].asSpec;
                };
                mapped = spec.map(val/127);
                node.set(ctrl, mapped);
            }, ccNum:ccNums, chan:ccChan)
            .fix;

            // initialize midi cc value
            // not sure how to find the correct midiout
            // so trying all of them
            MIDIClient.destinations.do({|dest, i|
                order.indices.do({|num|
                    var ctrl = order[num];
                    var spec = node.getSpec(ctrl);
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
                            MIDIOut(i).control(ccChan, num, ccval);
                        } {|err|
                            "midi out: %".format(err).warn;
                        }
                    }
                });
            })
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
        MIDIClient.init(verbose:true);
    }
}

