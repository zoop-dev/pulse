meta:
  id: duml_wifi
  title: DUML Wi-Fi command set
  endian: le
  -duml-cmdset: 0x07

doc: |
  Commands under cmdSet=WIFI(0x07).

types:
  set_pairing_pin_request:
    meta:
      -duml-cmd: 0x45
    doc: |
      Phone sends a hardcoded app-identifier string plus a PIN.
    seq:
      - id: id_len
        type: u1
      - id: id
        type: str
        size: id_len
        encoding: UTF-8
      - id: pin_len
        type: u1
      - id: pin
        type: str
        size: pin_len
        encoding: UTF-8

  set_pairing_pin_response:
    meta:
      -duml-cmd: 0x45
    doc: |
      Device replies with a status byte and a "pairing state" flag. If that flag is 0x01, no further pairing stages are needed - otherwise the app waits for a separate pairing_pin_approved push once a human confirms pairing on the device itself.
    seq:
      - id: status
        doc: usually 0x00
        type: u1
      - id: pairing_state
        doc: 0x01 if already paired, 0x02 if the user must approve the pairing.
        type: u1

  pairing_pin_approved:
    meta:
      -duml-cmd: 0x46
    seq:
      - id: status
        type: u1
